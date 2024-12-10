import React, {useEffect} from 'react';
import {Dimensions, StyleSheet, Text, TouchableOpacity} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {getStatusBarHeight, useStateWithCallback} from '../utils/Helper';
import Orientation from 'react-native-orientation-locker';
import LinearGradient from 'react-native-linear-gradient';
import {NativeEventEmitter, NativeModules} from 'react-native';

const {CastModule} = NativeModules;
const eventEmitter = new NativeEventEmitter(CastModule);

const TopBar = ({onResetHideTimer, onFullScreen, videoUrl, currentTime}) => {

  const [screenWidth, setScreenWidth] = useStateWithCallback(Dimensions.get('window').width);
  const [isFullscreen, setIsFullscreen] = useStateWithCallback(false);

  const enterFullscreen = (status) => {
    if (status) {
      Orientation.lockToLandscape(); // Yatay moda geçiş
    } else {
      Orientation.lockToPortrait(); // Dikey moda dönüş
    }
    setIsFullscreen(status, () => onFullScreen(status));
  };

  const onCast = () => {
    if (Platform.OS === 'ios') {
      // iOS için AirPlay picker'ı çağır
      if (CastModule.showAirPlayPickerDirectly) {
        CastModule.showAirPlayPickerDirectly(
            () => console.log('AirPlay picker displayed successfully on iOS'),
            (error) => console.error('Error displaying AirPlay picker on iOS:', error),
        );
      } else {
        console.error('showAirPlayPickerDirectly method is not defined on iOS');
      }
    } else if (Platform.OS === 'android') {
      // Android için casting başlat
      if (CastModule.showCastPicker) {
        CastModule.showCastPicker(
            () => console.log('Casting started successfully on Android'),
            (error) => console.error('Error starting casting on Android:', error),
        );
      } else {
        console.error('startCasting method is not defined on Android');
      }
    } else {
      console.error('Unsupported platform');
    }
    console.log('CastModule executed');
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

  return (
      <LinearGradient
          colors={['rgba(0, 0, 0, 0.7)', 'rgba(0, 0, 0, 0)']}
          style={[styles.container, {width: screenWidth, paddingTop: isFullscreen ? 20 : getStatusBarHeight()}]}
      >
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

        <TouchableOpacity
            style={styles.button}
            onPress={(event) => {
              enterFullscreen(!isFullscreen);
            }}
        >
          <Icon name="fullscreen" size={25} color="#fff"/>
          <Text style={styles.buttonText}>Tam Ekran</Text>
        </TouchableOpacity>
      </LinearGradient>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    paddingTop: '2%',
    paddingLeft: '2%',
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 10,

  },
  button: {
    alignItems: 'center',
    padding: 10,
  },
  buttonText: {
    color: '#fff',
    fontSize: 12,
  },
});

export default TopBar;
