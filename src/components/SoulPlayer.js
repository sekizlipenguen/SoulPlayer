import React, {forwardRef, useEffect, useRef, useState} from 'react';
import {ActivityIndicator, Animated, Dimensions, ImageBackground, Platform, StyleSheet, Text, TouchableOpacity, View} from 'react-native';

import Slider from '@react-native-community/slider';
import Video from 'react-native-video';
import Icon from 'react-native-vector-icons/MaterialIcons';
import SettingsMenu from './SettingsMenu';
import {useStateWithCallback} from '../utils/Helper';
import TopBar from './TopBar';

const platform = Platform.OS;

const SoulPlayer = forwardRef((props, ref) => {

    const {
        videoUrl,
        videoType = undefined,
        onLoadStart,
        onError,
        onEnd,
        onSeek,
        onProgress,
        onLoad,
        onMuteToggle,
        onQualityChange,
        onFullScreen,
        onPlay,
        onPause,
        onBackButton = null,
        showBackButton = false,
        paused,
    } = props;

    const videoRef = useRef(null);
    const fadeAnim = useRef(new Animated.Value(1)).current;
    const volumeIconRef = useRef(null);

    const [isPlaying, setIsPlaying] = useStateWithCallback(!paused);
    const [isMuted, setIsMuted] = useState(false);
    const [isLoading, setIsLoading] = useStateWithCallback(true);
    const [currentTime, setCurrentTime] = useStateWithCallback(0);
    const [duration, setDuration] = useStateWithCallback(0);
    const [showControls, setShowControls] = useStateWithCallback(true);
    const [volume, setVolume] = useState(0.5);
    const [isVolumeBarVisible, setIsVolumeBarVisible] = useState(false);
    const [hideTimeout, setHideTimeout] = useState(null);
    const [volumeBarPosition, setVolumeBarPosition] = useState({left: 0, bottom: 0});

    const [playbackRate, setPlaybackRate] = useState(1.0);
    const [isSettingsVisible, setIsSettingsVisible] = useStateWithCallback(false);

    const [isFullscreen, setIsFullscreen] = useStateWithCallback(false);

    const [selectedMaxBitRate, setSelectedMaxBitRate] = useStateWithCallback(0); // Varsayılan çözünürlük
    const [selectedAudioTrack, setSelectedAudioTrack] = useState(null); // Varsayılan ses parçası
    const [key, setKey] = useStateWithCallback(0);

    const screenWidth = Dimensions.get('window').width;


    React.useImperativeHandle(ref, () => ({
        getInfo: () => {
            return {
                duration,
                currentTime,
            };
        },
        videoRef: () => {
            return videoRef.current;
        },
    }));

    const togglePlayPause = () => {
        const newPlayingState = !isPlaying;
        resetHideTimer();
        setIsPlaying(newPlayingState, () => {
            // onPlay veya onPause tetikle
            if (newPlayingState && onPlay) {
                onPlay(currentTime);
            } else if (!newPlayingState && onPause) {
                onPause(currentTime);
            }
        });
    };

    const toggleMute = () => {
        resetHideTimer();
        const newMutedState = !isMuted;
        setIsMuted(newMutedState);

        if (onMuteToggle) {
            onMuteToggle(newMutedState);
        }
    };

    const formatTime = (time) => {
        const minutes = Math.floor(time / 60);
        const seconds = Math.floor(time % 60);
        return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
    };

    const handleProgress = (progress) => {
        setCurrentTime(progress.currentTime);
        if (onProgress) {
            onProgress(progress);
        }
    };

    const handleLoad = (data) => {
        setIsLoading(false, () => {
            setDuration(data.duration);
        });
        if (onLoad) {
            onLoad(data);
        }

    };

    const handleSeek = (value) => {
        videoRef.current.seek(value);
        setCurrentTime(value);
        resetHideTimer();
    };

    const showVolumeBar = () => {
        if (volumeIconRef.current) {
            volumeIconRef.current.measure((fx, fy, width, height, px, py) => {
                const {height: windowHeight} = Dimensions.get('window');
                setVolumeBarPosition({
                    left: px - width,
                    bottom: windowHeight - py + height + 10,
                });
            });
        }
        setIsVolumeBarVisible(true);
        resetHideTimer();

    };

    const hideVolumeBar = () => {
        setIsVolumeBarVisible(false);
    };

    const resetHideTimer = () => {
        if (hideTimeout) {
            clearTimeout(hideTimeout); // Mevcut zamanlayıcıyı temizle
        }

        const timer = setTimeout(() => {
            if (showControls) { // Eğer kontroller zaten gizli değilse
                Animated.timing(fadeAnim, {
                    toValue: 0, // Opaklığı 0 yap
                    duration: 300,
                    useNativeDriver: true,
                }).start(() => {
                    setShowControls(false); // Kontrolleri kapat
                });
            }
        }, 3000);

        setHideTimeout(timer);
    };

    const toggleControls = () => {
        if (showControls) {
            Animated.timing(fadeAnim, {
                toValue: 0, // Opaklığı 0 yap
                duration: 300,
                useNativeDriver: true,
            }).start(() => {
                setShowControls(false); // Kontrolleri kapat
            });
        } else {
            setShowControls(true); // Kontrolleri aç
            Animated.timing(fadeAnim, {
                toValue: 1, // Opaklığı 1 yap
                duration: 300,
                useNativeDriver: true,
            }).start(() => {
                resetHideTimer(); // Timer sıfırla
            });
        }
    };

    const toggleSettings = () => {
        setIsSettingsVisible(!isSettingsVisible);
    };

    const selectPlaybackRate = (rate) => {
        setPlaybackRate(rate);
    };

    const selectQuality = (itemQuality, audioTracks) => {
        setIsLoading(true);
        if (itemQuality && itemQuality.audioGroupId) {
            setSelectedAudioTrack({
                type: itemQuality.audioGroupId,
            });
        }
        setKey(Date.now().toString(), () => {
            setIsSettingsVisible(false, () => {
                const bandwidth = itemQuality && itemQuality.bandwidth ? itemQuality.bandwidth : 0;
                setSelectedMaxBitRate(parseInt(bandwidth), () => {
                    setTimeout(() => {
                        videoRef.current.seek(currentTime);
                        setIsLoading(false);
                        if (onQualityChange) {
                            onQualityChange(itemQuality, audioTracks);
                        }
                    }, 300); // 300ms gecikme ile seek yap
                });
            });
        });
    };

    useEffect(() => {
        resetHideTimer();
        return () => hideTimeout && clearTimeout(hideTimeout);
    }, []);

    return (
        <TouchableOpacity
            activeOpacity={1}
            style={styles.container}
            onPress={() => {
                if (isVolumeBarVisible) {
                    hideVolumeBar();
                } else {
                    toggleControls();
                    resetHideTimer();
                }
            }}
        >

            <Animated.View
                style={[styles.topBar, {opacity: fadeAnim}]}
                pointerEvents={showControls ? 'auto' : 'none'} // Etkileşim kontrolü
            >
                <TopBar
                    onResetHideTimer={resetHideTimer}
                    onFullScreen={(status) => {
                        setIsFullscreen(status, () => {
                            if (onFullScreen) {
                                onFullScreen(status);
                            }
                        });
                    }}
                    videoUrl={videoUrl}
                    currentTime={currentTime}
                    showBackButton={showBackButton}
                    onBackButton={onBackButton}
                />
            </Animated.View>

            <Video
                ref={videoRef}
                source={{uri: videoUrl, type: videoType}}
                style={styles.video}
                resizeMode="contain"
                paused={!isPlaying}
                muted={isMuted}
                volume={volume}
                rate={playbackRate}
                onLoad={handleLoad}
                onProgress={handleProgress}
                selectedAudioTrack={selectedAudioTrack}
                maxBitRate={selectedMaxBitRate}
                key={key}
                onLoadStart={() => onLoadStart && onLoadStart()}
                onError={(error) => onError && onError(error)}
                onEnd={() => onEnd && onEnd()}
                onSeek={(event) => onSeek && onSeek(event.seekTime)}
                {...props.videoProps}
            />

            {
                isLoading && (
                    <ActivityIndicator size="large" color="#fff" style={styles.loader}/>
                )
            }

            <Animated.View
                style={[styles.controlsContainer, {opacity: fadeAnim}]}
                pointerEvents={showControls ? 'auto' : 'none'}
            >
                {
                    isVolumeBarVisible && (
                        <View
                            style={[
                                styles.volumeSliderContainer,
                                {
                                    left: volumeBarPosition.left,
                                    bottom: volumeBarPosition.bottom,
                                    zIndex: 3,
                                    elevation: 5, // Android için
                                },
                            ]}
                        >
                            <View style={styles.volumeBackground}/>
                            <Slider
                                style={styles.volumeSlider}
                                minimumValue={0}
                                maximumValue={1}
                                value={volume}
                                onValueChange={(value) => {
                                    setVolume(value);
                                    resetHideTimer();
                                }}
                                minimumTrackTintColor="#00ff00"
                                maximumTrackTintColor="#fff"
                                thumbTintColor="#00ff00"
                            />
                        </View>
                    )
                }
                <ImageBackground
                    source={require('../styles/img/bottom-vignette.png')}
                    imageStyle={{
                        resizeMode: 'stretch',
                    }}
                    style={{
                        paddingBottom: (isFullscreen && platform != 'ios') ? 30 : 15,
                        paddingTop: 15,
                        zIndex: 1,
                    }}
                >
                    <Slider
                        style={styles.progressBar}
                        minimumValue={0}
                        maximumValue={duration}
                        value={currentTime}
                        onValueChange={handleSeek}
                        minimumTrackTintColor="#00ff00"
                        maximumTrackTintColor="#fff"
                        thumbTintColor="#00ff00"
                    />

                    <View style={styles.controls}>
                        <TouchableOpacity
                            ref={volumeIconRef}
                            onLongPress={showVolumeBar}
                            onPress={() => {
                                if (isVolumeBarVisible) {
                                    hideVolumeBar();
                                } else {
                                    toggleMute();
                                }
                            }}
                            style={styles.controlButton}
                        >
                            <Icon name={isMuted ? 'volume-off' : 'volume-up'} size={30} color="#fff"/>
                        </TouchableOpacity>

                        <TouchableOpacity
                            onPress={() => handleSeek(currentTime - 10)}
                            style={styles.controlButton}
                        >
                            <Icon name="replay-10" size={30} color="#fff"/>
                        </TouchableOpacity>

                        <TouchableOpacity onPress={togglePlayPause} style={styles.controlButton}>
                            <Icon name={isPlaying ? 'pause' : 'play-arrow'} size={40} color="#fff"/>
                        </TouchableOpacity>

                        <TouchableOpacity
                            onPress={() => handleSeek(currentTime + 10)}
                            style={styles.controlButton}
                        >
                            <Icon name="forward-10" size={30} color="#fff"/>
                        </TouchableOpacity>

                        <TouchableOpacity onPress={toggleSettings} style={styles.controlButton}>
                            <Icon name="settings" size={30} color="#fff"/>
                        </TouchableOpacity>
                    </View>

                    <View style={styles.timeContainer}>
                        <Text style={styles.time}>{formatTime(currentTime)}</Text>
                        <Text style={styles.time}>{formatTime(duration)}</Text>
                    </View>
                </ImageBackground>
            </Animated.View>

            <SettingsMenu
                visible={isSettingsVisible}
                onClose={toggleSettings}
                playbackRate={playbackRate}
                onSpeedChange={selectPlaybackRate}
                videoUrl={videoUrl}
                onQualityChange={selectQuality}
            />
        </TouchableOpacity>
    );
});

const styles = StyleSheet.create({
    container: {flex: 1, backgroundColor: '#000'},
    video: {width: '100%', height: '100%'},
    loader: {position: 'absolute', top: '50%', left: '50%', marginLeft: -20, marginTop: -20},
    controlsContainer: {
        position: 'absolute',
        bottom: 0,
        width: '100%',
    },
    controls: {
        flexDirection: 'row',
        justifyContent: 'space-evenly',
        alignItems: 'center',
        paddingVertical: 0,
    },
    controlButton: {padding: 10},
    progressBar: {marginLeft: '5%', width: '90%', height: 20, marginBottom: 0},
    volumeSliderContainer: {
        position: 'absolute',
        alignItems: 'center',
        justifyContent: 'center', // Merkezleme
        borderRadius: 8,
    },
    volumeBackground: {
        position: 'absolute',
        width: 60,
        height: 150,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        borderRadius: 8,
    },
    volumeSlider: {width: 150, height: 5, transform: [{rotate: '-90deg'}]},
    timeContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginLeft: '10%',
        width: '80%',
    },
    time: {color: '#fff', fontSize: 14},

    topBar: {
        position: 'absolute',
        top: 0,
        width: '100%',
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingHorizontal: 10,
        paddingVertical: 5,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        zIndex: 10,
    },
    topBarButton: {
        alignItems: 'center',
        marginHorizontal: 5,
    },
    topBarText: {
        color: '#fff',
        fontSize: 12,
    },

});

export default SoulPlayer;

