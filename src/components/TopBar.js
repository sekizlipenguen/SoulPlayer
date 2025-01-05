import React, {useEffect} from 'react';
import {Dimensions, ImageBackground, NativeEventEmitter, NativeModules, StatusBar, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {getStatusBarHeight, Icon, useStateWithCallback} from '../utils/Helper';

const {CastModule, SoulOrientationModule} = NativeModules;
// Native MediaRouteButton bileşeni

const castEventEmitter = new NativeEventEmitter(CastModule);

const TopBar = ({
  isPlaying,
  currentTime,
  handleSeek,
  videoUrl,
  onResetHideTimer,
  onFullScreen,
  showBackButton = false,
  onBackButton = null,
  isLoading = false,
  setIsLoading = null,
  onIsAndroidCastConnected = null,
  onTogglePlayPause,
}) => {
  const [screenWidth, setScreenWidth] = useStateWithCallback(Dimensions.get('window').width);
  const [isFullscreen, setIsFullscreen] = useStateWithCallback(false);
  const [isCastPlaying, setIsCastPlaying] = useStateWithCallback(isPlaying);

  const [isAndroidCastConnected, setIsAndroidCastConnected] = useStateWithCallback(false);

  const enterFullscreen = (status) => {
    StatusBar.setHidden(status, 'slide');
    if (status) {
      SoulOrientationModule.lockToLandscape();
    } else {
      SoulOrientationModule.lockToPortrait();
    }
    setIsFullscreen(status, () => onFullScreen(status));
  };

  const onCast = () => {
    if (Platform.OS === 'ios') {
      if (CastModule.showAirPlayPickerDirectly) {
        CastModule.showAirPlayPickerDirectly(
            () => console.log('AirPlay picker displayed successfully on iOS'),
            (error) => console.error('Error displaying AirPlay picker on iOS:', error),
        );
      } else {
        console.error('showAirPlayPickerDirectly method is not defined on iOS');
      }
    } else if (Platform.OS === 'android') {
      if (isAndroidCastConnected) {
        CastModule.showControllerDialog();
      } else {
        CastModule.showCastDialog();
      }
    } else {
      console.error('Unsupported platform');
    }
  };

  useEffect(() => {
    const updateDimensions = () => {
      const {width} = Dimensions.get('window');
      setScreenWidth(width);
    };
    const subscription = Dimensions.addEventListener('change', updateDimensions);
    return () => {
      subscription?.remove();
    };
  }, []);

  useEffect(() => {
    CastModule.seekTo(currentTime);
  }, [currentTime]);

  useEffect(() => {
    if (isAndroidCastConnected) {
      if (isPlaying) {
        CastModule.play();
      } else {
        CastModule.pause();
      }
    }
  }, [isPlaying]);

  useEffect(() => {

    const onSessionStartingListener = castEventEmitter.addListener('onSessionStarting', () => {
      console.log('Session starting...');
      setIsLoading(true); // Cihaz seçildiğinde loading başlat
      setIsAndroidCastConnected(true, () => onIsAndroidCastConnected(true));
    });

    const onSessionStartedListener = castEventEmitter.addListener('onSessionStarted', () => {
      console.log('Cast session started');
      onIsAndroidCastConnected(true);
      setIsLoading(false); // Cast başladığında loading kapat
      CastModule.playMedia(
          videoUrl,
          null,
          null,
      );
      CastModule.seekTo(currentTime);
      onTogglePlayPause(true);
    });

    const onSessionEndedListener = castEventEmitter.addListener('onSessionEnded', () => {
      setIsLoading(false); // Cast sona erdiğinde loading kapat
      setIsAndroidCastConnected(false, () => {
        onIsAndroidCastConnected(false);
      });
    });

    const onSessionEndingListener = castEventEmitter.addListener('onSessionEnding', (raw) => {
      const info = JSON.parse(raw);
      if (handleSeek && info?.currentTime) {
        handleSeek(info?.currentTime);
      }
    });

    return () => {
      // Eventleri temizle
      onSessionStartingListener.remove();
      onSessionStartedListener.remove();
      onSessionEndedListener.remove();
      onSessionEndingListener.remove();
    };
  }, []);

  return (
      <>
        <ImageBackground
            source={require('../styles/img/top-vignette.png')}
            style={[styles.container, {width: screenWidth, paddingTop: isFullscreen ? 20 : getStatusBarHeight()}]}
            imageStyle={styles.vignette}
        >
          <View style={styles.leftButtons}>
            {
                showBackButton && (
                    <>
                      <TouchableOpacity
                          style={styles.button}
                          onPress={() => {
                            if (isFullscreen) {
                              enterFullscreen(false);
                            }
                            if (onBackButton) {
                              onBackButton();
                            }
                          }}
                      >
                        <Icon name="arrow-back" size={25} color="#fff"/>
                        <Text style={styles.buttonText}>Geri</Text>
                      </TouchableOpacity>

                    </>
                )
            }

          </View>

          <View style={styles.rightButtons}>

            <TouchableOpacity
                style={styles.button}
                onPress={() => {
                  enterFullscreen(!isFullscreen);
                }}
            >
              <Icon name={isFullscreen ? 'fullscreen-exit' : 'fullscreen'} size={30} color="#fff"/>
              <Text style={styles.buttonText}>{isFullscreen ? 'Normal Ekran' : 'Tam Ekran'}</Text>
            </TouchableOpacity>

            {/* Yansıt Butonu */}
            <TouchableOpacity
                style={[styles.button, {marginLeft: 3}]}
                onPress={() => {
                  onCast();
                  onResetHideTimer();
                }}
            >
              <Icon name={isAndroidCastConnected ? 'cast-connected' : 'cast'} size={22} color="#fff"/>
              <Text style={[styles.buttonText, {paddingLeft: 5}]}>{isAndroidCastConnected ? 'Durdur' : 'Yansıt'}</Text>
            </TouchableOpacity>

          </View>

        </ImageBackground>
      </>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 10,
  },
  leftButtons: {
    flexDirection: 'column',
    alignItems: 'flex-end',
    alignSelf: 'flex-start',
  },
  rightButtons: {
    flexDirection: 'column',
    alignItems: 'flex-start',
  },
  button: {
    alignItems: 'center',
    marginRight: 10,
    flexDirection: 'row',
    padding: 10,
  },
  buttonText: {
    color: '#fff',
    fontSize: 12,
    marginLeft: 5,
  },
  vignette: {
    resizeMode: 'stretch',
  },
  castButton: {
    width: 50,
    height: 50,
  },
});

export default TopBar;
