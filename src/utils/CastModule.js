import {NativeEventEmitter, NativeModules} from 'react-native';

const {CastModule} = NativeModules;
const eventEmitter = new NativeEventEmitter(CastModule);

// Olayları dinleme
const startListener = eventEmitter.addListener('onCastStart', () => {
  console.log('Casting started');
});

const stopListener = eventEmitter.addListener('onCastStop', () => {
  console.log('Casting stopped');
});

// AirPlay ile casting başlatma
CastModule.startCastingWithAirPlay(
    () => console.log('AirPlay casting started successfully'),
    (error) => console.error('Casting error:', error),
);

// Casting durdurma
CastModule.stopCasting(
    () => console.log('Casting stopped successfully'),
    (error) => console.error('Casting stop error:', error),
);

// Unmount sırasında dinleyicileri kaldır
startListener.remove();
stopListener.remove();
