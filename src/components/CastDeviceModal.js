import React, {useEffect, useState} from 'react';
import {ActivityIndicator, Alert, FlatList, Modal, NativeModules, PermissionsAndroid, Platform, StatusBar, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import Connection from '@sekizlipenguen/connection';

const {CastModule} = NativeModules;

async function requestLocationPermission() {
  if (Platform.OS === 'android') {
    try {
      const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          {
            title: 'Konum İzni Gerekli',
            message: 'Cihazları tarayabilmek için konum izni vermeniz gerekiyor.',
            buttonNeutral: 'Daha Sonra Sor',
            buttonNegative: 'İptal',
            buttonPositive: 'Tamam',
          },
      );

      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        console.log('Konum izni verildi.');
        return true;
      } else {
        console.log('Konum izni reddedildi.');
        return false;
      }
    } catch (err) {
      console.warn(err);
      return false;
    }
  } else {
    return true; // iOS cihazlarda ek bir izin gerekmez
  }
}

const CastDeviceModal = ({visible, onClose, isFullscreen, videoUrl}) => {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [intervalId, setIntervalId] = useState(null);

  useEffect(() => {
    const initialize = async () => {
      if (visible) {
        const hasPermission = await requestLocationPermission();
        if (hasPermission) {
          StatusBar.setHidden(isFullscreen, 'slide');
          fetchDevices();
          const id = setInterval(fetchDevices, 60000); // 15 saniyede bir cihazları güncelle
          setIntervalId(id);
        } else {
          console.log('Konum izni olmadan cihaz taranamaz.');
        }
      } else {
        clearTimer();
      }
    };

    initialize();

    return () => {
      clearTimer();
    };
  }, [visible]);

  const clearTimer = () => {
    if (intervalId) {
      clearInterval(intervalId);
      setIntervalId(null);
    }
  };

  const fetchDevices = async () => {
    setLoading(true);
    if (Platform.OS === 'android') {
      try {
        const routes = await CastModule.scanForDevices(60000); // 15 saniyelik timeout
        const parsedRoutes = JSON.parse(routes);
        console.log('Tarama sonuçları:', parsedRoutes);
        const googleCastDevices = parsedRoutes.googleCastDevices || [];
        const airPlayDevices = parsedRoutes.airPlayDevices || [];

        const allDevices = [...googleCastDevices, ...airPlayDevices];
        setDevices(allDevices);
      } catch (error) {
        console.error('Cihaz tarama hatası:', error);
      } finally {
        setLoading(false);
      }
    } else {
      setLoading(false);
    }
  };

  const selectDevice = async (device) => {
    try {
      console.log(`Seçilen cihaz: ${device.name}`);
      const deviceType = device.port === 7000 ? 'airplay' : 'googlecast';
      console.log(
          deviceType,
          device.address,
          device.port,
          videoUrl,
      );

      const response = await Connection.post('http://192.168.1.75:45102/play', {
        'Content-Location': 'https://content.jwplatform.com/manifests/vM7nH0Kl.m3u8',
        'Start-Position': 0,
      }, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Basic iKrtR7dkjfVCb/IKUxSw4VbiUtfvt5Z2g+pRLJX962E=',
        },
      });
      console.log('response', response);
      /*await CastModule.sendMediaToDevice(
        deviceType,
        device.address,
        device.port,
        videoUrl,
      );*/

      Alert.alert('Başarılı', `${device.name} cihazına medya gönderildi.`);
      onClose();
    } catch (error) {
      console.error('Medya gönderimi hatası:', error);
      Alert.alert('Hata', 'Medya gönderimi sırasında bir hata oluştu.');
    }
  };

  return (
      <Modal visible={visible} animationType="slide" transparent>
        <View style={styles.modalContainer}>
          <Text style={styles.modalTitle}>Cihazlar</Text>
          {loading ? (
              <ActivityIndicator size="large" color="#0000ff"/>
          ) : devices.length > 0 ? (
              <FlatList
                  data={devices}
                  keyExtractor={(item, index) => `${item.name}_${item.address}_${item.port}_${index}`}
                  renderItem={({item}) => (
                      <TouchableOpacity
                          style={styles.deviceItem}
                          onPress={() => selectDevice(item)}
                      >
                        <Text style={styles.deviceName}>{item.name}</Text>
                        <Text style={styles.deviceDescription}>
                          {item.address}:{item.port}
                        </Text>
                      </TouchableOpacity>
                  )}
              />
          ) : (
              <View style={styles.noDevicesContainer}>
                <Text style={styles.noDevices}>
                  Cihaz bulunamadı. Taranıyor...
                </Text>
              </View>
          )}
          <TouchableOpacity onPress={onClose} style={styles.closeButton}>
            <Text style={styles.closeButtonText}>Kapat</Text>
          </TouchableOpacity>
        </View>
      </Modal>
  );
};

const styles = StyleSheet.create({
  modalContainer: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalTitle: {
    fontSize: 20,
    color: '#fff',
    marginBottom: 20,
  },
  deviceItem: {
    padding: 15,
    borderBottomWidth: 1,
    borderBottomColor: '#ccc',
    width: '90%',
    alignItems: 'center',
  },
  deviceName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#fff',
  },
  deviceDescription: {
    fontSize: 14,
    color: '#ccc',
  },
  noDevicesContainer: {
    alignItems: 'center',
  },
  noDevices: {
    fontSize: 16,
    color: '#fff',
    marginTop: 20,
  },
  closeButton: {
    marginTop: 20,
    padding: 10,
    backgroundColor: '#ff0000',
    borderRadius: 5,
  },
  closeButtonText: {
    color: '#fff',
    fontSize: 16,
  },
});

export default CastDeviceModal;
