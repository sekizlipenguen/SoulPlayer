import {useEffect, useRef, useState} from 'react';
import {Animated, Dimensions, Image, Platform, StatusBar} from 'react-native';

export function useStateWithCallback(initialValue) {
  const [state, setState] = useState(initialValue);
  const callbackRef = useRef(null);

  const setStateWithCallback = (value, callback) => {
    callbackRef.current = callback; // Callback'i kaydediyoruz
    setState(value);
  };

  useEffect(() => {
    if (callbackRef.current) {
      callbackRef.current(state);
      callbackRef.current = null; // Callback'i temizliyoruz
    }
  }, [state]);

  return [state, setStateWithCallback];
}

// Cihaz Boyutları ve Status Bar Yükseklikleri
const DEVICE_SPECS = [
  {width: 375, height: 812, statusBarHeight: 44}, // iPhone X
  {width: 414, height: 896, statusBarHeight: 44}, // iPhone XS Max
  {width: 390, height: 844, statusBarHeight: 47}, // iPhone 12
  {width: 428, height: 926, statusBarHeight: 47}, // iPhone 12 Max
  {width: 393, height: 852, statusBarHeight: 49}, // iPhone 14 Pro
  {width: 430, height: 932, statusBarHeight: 49}, // iPhone 14 Pro Max
  {width: 396, height: 852, statusBarHeight: 50}, // iPhone 15
  {width: 430, height: 932, statusBarHeight: 50}, // iPhone 15 Max
  {width: 402, height: 874, statusBarHeight: 51}, // iPhone 16 Pro
  {width: 440, height: 956, statusBarHeight: 51}, // iPhone 16 Pro Max
];

const {height: W_HEIGHT, width: W_WIDTH} = Dimensions.get('window');

let statusBarHeight = 20; // Varsayılan Status Bar Yüksekliği
let isIPhoneWithMonobrow_v = false;

// Cihaz Boyutlarını Kontrol Et
if (Platform.OS === 'ios' && !Platform.isPad && !Platform.isTVOS) {
  const device = DEVICE_SPECS.find(
      (d) => d.width === W_WIDTH && d.height === W_HEIGHT,
  );

  if (device) {
    statusBarHeight = device.statusBarHeight;
    isIPhoneWithMonobrow_v = true;
  }
}

// Monobrow Kontrol Fonksiyonu
export const isIPhoneWithMonobrow = () => isIPhoneWithMonobrow_v;

// Status Bar Yüksekliği Fonksiyonu
export function getStatusBarHeight(skipAndroid = false) {
  return Platform.select({
    ios: statusBarHeight,
    android: skipAndroid ? 0 : StatusBar.currentHeight,
    default: 0,
  });
}

/**
 * Fade-in animasyonu
 * @param {Animated.Value} animatedValue - Animasyon yapılacak değer
 * @param {number} duration - Animasyon süresi (ms)
 */
export const fadeIn = (animatedValue, duration = 300) => {
  Animated.timing(animatedValue, {
    toValue: 1, // Opak görünüm
    duration: duration,
    useNativeDriver: true,
  }).start();
};

/**
 * Fade-out animasyonu
 * @param {Animated.Value} animatedValue - Animasyon yapılacak değer
 * @param {Function} onComplete - Animasyon tamamlandığında çalışacak callback
 * @param {number} duration - Animasyon süresi (ms)
 */
export const fadeOut = (animatedValue, onComplete, duration = 300) => {
  Animated.timing(animatedValue, {
    toValue: 0, // Tamamen görünmez
    duration: duration,
    useNativeDriver: true,
  }).start(() => {
    if (onComplete) {
      onComplete();
    }
  });
};

export const Icon = ({name, size = 30}) => {
  const iconMap = {
    'arrow-back': require('../styles/icons/arrow_back.png'),
    'fullscreen': require('../styles/icons/fullscreen.png'),
    'fullscreen-exit': require('../styles/icons/fullscreen_exit.png'),
    'cast': require('../styles/icons/cast.png'),
    'cast-connected': require('../styles/icons/cast_connected.png'),
    'volume-off': require('../styles/icons/volume_of.png'),
    'volume-up': require('../styles/icons/volume_up.png'),
    'replay-10': require('../styles/icons/replay_10.png'),
    'forward-10': require('../styles/icons/forward_10.png'),
    'pause': require('../styles/icons/pause.png'),
    'pause-circle': require('../styles/icons/pause_circle.png'),
    'play-arrow': require('../styles/icons/play_arrow.png'),
    'settings': require('../styles/icons/settings.png'),
  };

  const iconSource = iconMap[name];

  if (!iconSource) {
    console.warn(`Icon "${name}" not found`);
    return null;
  }

  return (
      <Image
          source={iconSource}
          style={{
            width: size,
            height: size,
          }}
          resizeMode="contain"
      />
  );
};
