module.exports = {
  dependency: {
    platforms: {
      ios: {}, // iOS için otomatik bağlantı
      android: {
        packageInstance: 'new CastPackage()', // Android modülü
      },
    },
  },
};
