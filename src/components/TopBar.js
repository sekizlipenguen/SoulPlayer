import React, {useEffect} from 'react';
import {Dimensions, ImageBackground, NativeModules, StatusBar, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {getStatusBarHeight, useStateWithCallback} from '../utils/Helper';
import Orientation from 'react-native-orientation-locker';
import CastDeviceModal from '@sekizlipenguen/react-native-soul-player/src/components/CastDeviceModal';

const {CastModule} = NativeModules;

const TopBar = ({onResetHideTimer, onFullScreen, showBackButton = false, onBackButton = null}) => {
  const [screenWidth, setScreenWidth] = useStateWithCallback(Dimensions.get('window').width);
  const [isFullscreen, setIsFullscreen] = useStateWithCallback(false);
  const [isCastModalVisible, setCastModalVisible] = useStateWithCallback(false);

  const enterFullscreen = (status) => {
    StatusBar.setHidden(status, 'slide');
    if (status) {
      Orientation.lockToLandscape();
    } else {
      Orientation.lockToPortrait();
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
            {/* Geri Butonu (Props ile kontrol ediliyor) */}
            {showBackButton && (
                <TouchableOpacity
                    style={styles.button}
                    onPress={() => onBackButton && onBackButton()}
                >
                  <Icon name="arrow-back" size={25} color="#fff"/>
                  <Text style={styles.buttonText}>Geri</Text>
                </TouchableOpacity>
            )}

            {/* Yansıt Butonu */}
            <TouchableOpacity
                style={styles.button}
                onPress={() => {
                  onCast();
                  onResetHideTimer();
                }}
            >
              <Icon name="cast" size={25} color="#fff"/>
              <Text style={styles.buttonText}>Yansıt</Text>
            </TouchableOpacity>
          </View>

          {/* Tam Ekran Butonu */}
          <TouchableOpacity
              style={styles.button}
              onPress={() => {
                enterFullscreen(!isFullscreen);
              }}
          >
            <Icon name="fullscreen" size={25} color="#fff"/>
            <Text style={styles.buttonText}>Tam Ekran</Text>
          </TouchableOpacity>
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
    flexDirection: 'row',
    alignItems: 'center',
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
