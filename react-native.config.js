module.exports = {
  dependency: {
    platforms: {
      ios: {
        podspecPath: 'react-native-soul-player.podspec', // iOS için podspec dosyası
      },
      android: {
        packageInstance: 'new CastPackage()', // Android modülü
      },
    },
  },
};
