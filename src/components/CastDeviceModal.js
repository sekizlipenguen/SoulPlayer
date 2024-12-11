import React, {useState, useEffect} from 'react';
import {
  View,
  Text,
  Modal,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  Platform,
  NativeModules, StatusBar,
} from 'react-native';

const {CastModule} = NativeModules;

const CastDeviceModal = ({visible, onClose, isFullscreen}) => {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [intervalId, setIntervalId] = useState(null);

  useEffect(() => {
    if (visible) {
      StatusBar.setHidden(isFullscreen, 'slide');
      fetchDevices();
      const id = setInterval(() => {
        fetchDevices();
      }, 10000); // 10 saniyede bir cihazları güncelle
      setIntervalId(id);
    } else {
      if (intervalId) {
        clearInterval(intervalId);
        setIntervalId(null);
      }
    }

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [visible]);

  const fetchDevices = () => {
    setLoading(true);
    if (Platform.OS === 'android') {
      CastModule.scanForCastDevices().then((routes) => {
        console.log('rout', routes);
        const parsedRoutes = JSON.parse(routes);
        setDevices(parsedRoutes);
      }).catch((error) => console.error('Error getting routes:', error)).finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  };

  const selectDevice = (deviceName) => {
    CastModule.selectRoute(deviceName).then(() => {
      console.log(`Route selected: ${deviceName}`);
      onClose();
    }).catch((error) => console.error('Error selecting route:', error));
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
                  keyExtractor={(item) => item.name}
                  renderItem={({item}) => (
                      <TouchableOpacity
                          style={styles.deviceItem}
                          onPress={() => selectDevice(item.name)}
                      >
                        <Text style={styles.deviceName}>{item.name}</Text>
                        <Text style={styles.deviceDescription}>{item.description}</Text>
                      </TouchableOpacity>
                  )}
              />
          ) : (
              <View style={styles.noDevicesContainer}>
                <ActivityIndicator size="small" color="#fff"/>
                <Text style={styles.noDevices}>Cihaz bulunamadı, taranıyor...</Text>
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
