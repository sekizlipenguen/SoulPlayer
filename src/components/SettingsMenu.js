import React, {useEffect, useState} from 'react';
import {Modal, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {analyzeManifest} from '../utils/AnalyzeManifest';
import Slider from '@react-native-community/slider';

const formatSpeed = (speed) => {
  return Number.isInteger(speed) ? speed.toString() : parseFloat(speed.toFixed(2)).toString();
};

const SettingsMenu = ({visible, onClose, playbackRate, onSpeedChange, videoUrl, onQualityChange}) => {
  const [currentPlaybackRate, setCurrentPlaybackRate] = useState(playbackRate);
  const [qualities, setQualities] = useState([]);
  const [audioTracks, setAudioTracks] = useState([]);
  const [selectedQuality, setSelectedQuality] = useState('recommended'); // Default "Önerilen"

  const handleIncrease = () => {
    if (currentPlaybackRate < 2.0) {
      const newRate = Math.min(currentPlaybackRate + 0.05, 2.0);
      setCurrentPlaybackRate(newRate);
      onSpeedChange(newRate);
    }
  };

  const handleDecrease = () => {
    if (currentPlaybackRate > 0.25) {
      const newRate = Math.max(currentPlaybackRate - 0.05, 0.25);
      setCurrentPlaybackRate(newRate);
      onSpeedChange(newRate);
    }
  };

  const handleSliderChange = (value) => {
    setCurrentPlaybackRate(value);
    onSpeedChange(value);
  };

  const handleAnalyzeManifest = async () => {
    try {
      const result = await analyzeManifest(videoUrl);
      if (result.type === 'HLS') {
        const qualityOptions = result.streams.map((stream) => ({
          label: `${stream.QUALITY} (${stream.RESOLUTION})`,
          resolution: stream.RESOLUTION,
          quality: stream.QUALITY,
          width: stream.WIDTH,
          height: stream.HEIGHT,
          bandwidth: stream.BANDWIDTH,
          url: stream.URI,
          audioGroupId: stream.AUDIO, // Ses grubu ID'si
        })).sort((a, b) => a.height - b.height); // height'e göre artan sırala

        const audioOptions = result.metadata.map((audio) => ({
          groupId: audio['GROUP-ID'],
          uri: audio.URI,
        }));

        setQualities(qualityOptions);
        setAudioTracks(audioOptions); // Ses dosyalarını kaydet
        setSelectedQuality('recommended'); // Default "Önerilen"
      }
    } catch (error) {
      console.error('Manifest Analiz Hatası:', error.message);
    }
  };

  const handleQualitySelect = (quality) => {
    setSelectedQuality(quality);
    onQualityChange(
        quality === 'recommended' ? null : quality,
        audioTracks,
    );
  };

  useEffect(() => {
    handleAnalyzeManifest();
  }, [videoUrl]);

  return (
      <Modal
          visible={visible}
          transparent={true}
          animationType="fade"
          onRequestClose={onClose}
          supportedOrientations={['portrait', 'landscape']}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.settingsContainer}>
            {/* Oynatma Hızı Başlığı */}
            <Text style={styles.settingsTitle}>Oynatma Hızı</Text>

            {/* Slider ve Artırma/Azaltma Butonları */}
            <View style={styles.sliderContainer}>
              <TouchableOpacity
                  onPress={handleDecrease}
                  style={styles.adjustButton}
              >
                <Text style={styles.adjustText}>-</Text>
              </TouchableOpacity>

              <Slider
                  style={styles.slider}
                  minimumValue={0.25}
                  maximumValue={2.0}
                  step={0.05}
                  value={currentPlaybackRate}
                  onValueChange={handleSliderChange}
                  minimumTrackTintColor="#00ff00"
                  maximumTrackTintColor="#fff"
                  thumbTintColor="#00ff00"
              />

              <TouchableOpacity
                  onPress={handleIncrease}
                  style={styles.adjustButton}
              >
                <Text style={styles.adjustText}>+</Text>
              </TouchableOpacity>
            </View>

            {/* Hız Butonları */}
            <View style={styles.speedOptions}>
              {[0.25, 1.0, 1.25, 1.5, 2].map((speed) => (
                  <TouchableOpacity
                      key={speed}
                      onPress={() => {
                        setCurrentPlaybackRate(speed);
                        onSpeedChange(speed);
                      }}
                      style={[
                        styles.speedButton,
                        currentPlaybackRate === speed &&
                        styles.selectedSpeedButton,
                      ]}
                  >
                    <Text
                        style={[
                          styles.speedText,
                          currentPlaybackRate === speed &&
                          styles.selectedSpeedText,
                        ]}
                    >
                      {formatSpeed(speed)}x
                    </Text>
                  </TouchableOpacity>
              ))}
            </View>

            {/* Kalite Seçenekleri */}
            <Text style={styles.settingsTitle}>Görüntü Kalitesi</Text>
            <View style={styles.qualityContainer}>
              {/* Önerilen Butonu */}
              <TouchableOpacity
                  style={[
                    styles.qualityButton,
                    selectedQuality === 'recommended' && styles.selectedQualityButton,
                  ]}
                  onPress={() => handleQualitySelect('recommended')}
              >
                <Text
                    style={[
                      styles.qualityText,
                      selectedQuality === 'recommended' && styles.selectedQualityText,
                    ]}
                >
                  Önerilen
                </Text>
              </TouchableOpacity>

              {/* Diğer Kalite Butonları */}
              {qualities.map((item) => (
                  <TouchableOpacity
                      key={item.url}
                      style={[
                        styles.qualityButton,
                        selectedQuality.resolution === item.resolution && styles.selectedQualityButton,
                      ]}
                      onPress={() => handleQualitySelect(item)}
                  >
                    <Text
                        style={[
                          styles.qualityText,
                          selectedQuality?.resolution === item.resolution && styles.selectedQualityText,
                        ]}
                    >
                      {item.label}
                    </Text>
                  </TouchableOpacity>
              ))}
            </View>

            {/* Menüyü Kapatma */}
            <TouchableOpacity onPress={onClose} style={styles.closeButton}>
              <Text style={styles.closeText}>Kapat</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
  );
};

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  settingsContainer: {
    width: '90%',
    backgroundColor: '#222',
    borderRadius: 10,
    padding: 20,
    alignItems: 'center',
  },
  settingsTitle: {
    fontSize: 20,
    color: '#fff',
    marginBottom: 10,
  },
  sliderContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
  },
  slider: {
    flex: 1,
    marginHorizontal: 10,
  },
  adjustButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#444',
    justifyContent: 'center',
    alignItems: 'center',
  },
  adjustText: {
    fontSize: 20,
    color: '#fff',
  },
  speedOptions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 20,
  },
  speedButton: {
    padding: 8,
    borderRadius: 5,
    backgroundColor: '#444',
    marginHorizontal: 5,
  },
  selectedSpeedButton: {
    backgroundColor: '#00ff00',
  },
  speedText: {
    fontSize: 14,
    color: '#fff',
  },
  selectedSpeedText: {
    color: '#000',
  },
  qualityContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    marginVertical: 10,
  },
  qualityButton: {
    padding: 10,
    borderRadius: 5,
    backgroundColor: '#444',
    marginHorizontal: 5,
    marginVertical: 5,
  },
  selectedQualityButton: {
    backgroundColor: '#00ff00',
  },
  qualityText: {
    fontSize: 14,
    color: '#fff',
  },
  selectedQualityText: {
    color: '#000',
  },
  closeButton: {
    padding: 10,
    backgroundColor: '#00ff00',
    borderRadius: 5,
  },
  closeText: {
    color: '#000',
    fontSize: 16,
  },
});

export default SettingsMenu;
