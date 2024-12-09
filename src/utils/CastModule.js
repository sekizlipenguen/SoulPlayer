import {NativeModules} from 'react-native';

const {CastModule} = NativeModules;

export const startCasting = (url, title = 'Video', subtitle = '') => {
  return CastModule.startCasting(url, title, subtitle);
};

export const stopCasting = () => {
  return CastModule.stopCasting();
};

export const seekTo = (seconds) => {
  return CastModule.seekTo(seconds);
};

export const setPlaybackRate = (rate) => {
  return CastModule.setPlaybackRate(rate);
};
