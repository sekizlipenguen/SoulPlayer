import React, {useEffect} from 'react';
import {Dimensions, ImageBackground, NativeModules, StatusBar, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {getStatusBarHeight, useStateWithCallback} from '../utils/Helper';

import CastDeviceModal from '@sekizlipenguen/react-native-soul-player/src/components/CastDeviceModal';

const {CastModule, SoulOrientationModule} = NativeModules;

const TopBar = ({onResetHideTimer, onFullScreen, showBackButton = false, onBackButton = null}) => {
    const [screenWidth, setScreenWidth] = useStateWithCallback(Dimensions.get('window').width);
    const [isFullscreen, setIsFullscreen] = useStateWithCallback(false);
    const [isCastModalVisible, setCastModalVisible] = useStateWithCallback(false);

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
            setCastModalVisible(true);
        } else {
            console.error('Unsupported platform');
        }
    };

    const closeCastModal = () => setCastModalVisible(false);

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
                  <Icon name="fullscreen" size={30} color="#fff"/>
                  <Text style={styles.buttonText}>Tam Ekran</Text>
                </TouchableOpacity>

                    {/* Yansıt Butonu */}
                    <TouchableOpacity
                        style={[styles.button, {marginLeft: 3}]}
                        onPress={() => {
                            onCast();
                            onResetHideTimer();
                        }}
                    >
                      <Icon name="cast" size={22} color="#fff"/>
                      <Text style={[styles.buttonText, {paddingLeft: 5}]}>Yansıt</Text>
                    </TouchableOpacity>

                </View>

            </ImageBackground>
            <CastDeviceModal visible={isCastModalVisible} onClose={closeCastModal} isFullscreen={isFullscreen}/>
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
});

export default TopBar;
