import React, {useEffect, useState, useRef} from 'react';
import {ActivityIndicator, FlatList, Modal, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {NativeModules} from 'react-native';

const {CastModule} = NativeModules;

const CastDeviceModal = ({visible, onClose}) => {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(false);
  const intervalRef = useRef(null);

  useEffect(() => {
    if (visible) {
      startDeviceDiscovery();
    } else {
      stopDeviceDiscovery();
    }

    return () => {
      stopDeviceDiscovery();
    };
  }, [visible]);

  const startDeviceDiscovery = () => {
    fetchDevices();

    // Listeyi 10 saniyede bir yenile
    intervalRef.current = setInterval(fetchDevices, 1000000);
  };

  const stopDeviceDiscovery = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  };

  const fetchDevices = async () => {
    setLoading(true);
    try {
      const result = await CastModule.discoverDevices();
      const newDevices = JSON.parse(result);

      // Yeni cihazları ekleyip eski cihazları çıkar
      setDevices((prevDevices) => {
        const updatedDevices = newDevices.filter((newDevice) =>
            !prevDevices.find((prevDevice) => prevDevice.id === newDevice.id),
        );
        const removedDevices = prevDevices.filter((prevDevice) =>
            !newDevices.find((newDevice) => newDevice.id === prevDevice.id),
        );
        console.log('Yeni cihazlar:', updatedDevices);
        console.log('Silinen cihazlar:', removedDevices);
        return newDevices;
      });
    } catch (error) {
      console.error('Cihaz tarama hatası:', error);
    } finally {
      setLoading(false);
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
                  keyExtractor={(item) => item.id}
                  renderItem={({item}) => (
                      <TouchableOpacity style={styles.deviceItem}>
                        <Text style={styles.deviceName}>{item.name}</Text>
                        <Text style={styles.deviceDescription}>{item.description}</Text>
                      </TouchableOpacity>
                  )}
              />
          ) : (
              <Text style={styles.noDevices}>Cihaz bulunamadı. Tekrar deneniyor...</Text>
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
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalTitle: {
    fontSize: 18,
    color: '#fff',
    marginBottom: 20,
  },
  deviceItem: {
    padding: 15,
    borderBottomWidth: 1,
    borderBottomColor: '#ccc',
    width: '90%',
  },
  deviceName: {
    fontSize: 16,
    color: '#fff',
  },
  deviceDescription: {
    fontSize: 14,
    color: '#ccc',
  },
  noDevices: {
    fontSize: 16,
    color: '#fff',
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
