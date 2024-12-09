import Connection from '@sekizlipenguen/connection';
import xml2js from 'react-native-xml2js';

// Manifest indirme ve içeriği alma
const fetchManifest = async (url) => {
  try {
    const response = await Connection.get(url);
    const manifestText = await response.data.text();
    console.log('Manifest İçeriği:', manifestText);
    return manifestText;
  } catch (error) {
    throw new Error(`Manifest indirilemedi: ${error.message}`);
  }
};

// Manifest türünü belirleme
const determineManifestType = (content) => {
  if (content.startsWith('#EXTM3U')) {
    return 'HLS';
  }
  if (content.trim().startsWith('<MPD')) {
    return 'DASH';
  }
  return 'UNKNOWN';
};

// Kalite etiketini belirleme (ör. 360p, 480p, 2K, 4K)
const getQualityLabel = (resolution) => {
  if (!resolution) return 'Unknown';
  const [, height] = resolution.split('x').map(Number);
  return height + 'p';
};

// HLS stream'lerini ayrıştırma ve kalite ekleme
const parseHLSStreams = (lines, baseUrl) => {
  const streams = [];
  let lastStreamAttributes = null;

  lines.forEach((line) => {
    try {
      if (line.startsWith('#EXT-X-STREAM-INF')) {
        const attributes = {};
        line.match(/([A-Z\-]+)=([^,]+)/g)?.forEach((attr) => {
          const [key, value] = attr.split('=');
          attributes[key] = value.replace(/"/g, '');
        });
        lastStreamAttributes = attributes;
      } else if (lastStreamAttributes && line.trim() && !line.startsWith('#')) {
        lastStreamAttributes.URI = new URL(line.trim(), baseUrl).href;
        if (lastStreamAttributes.RESOLUTION) {
          const [width, height] = lastStreamAttributes.RESOLUTION.split('x').map(Number);
          lastStreamAttributes.QUALITY = getQualityLabel(lastStreamAttributes.RESOLUTION);
          lastStreamAttributes.WIDTH = width;
          lastStreamAttributes.HEIGHT = height;
        } else {
          lastStreamAttributes.QUALITY = 'Audio Only';
        }
        if (lastStreamAttributes.HEIGHT) {
          streams.push(lastStreamAttributes);
        }
        lastStreamAttributes = null;
      }
    } catch (error) {
      console.error(`Stream ayrıştırma hatası: ${error.message}`);
    }
  });

  return streams;
};

// HLS metadata'yı ayrıştırma
const parseHLSMetadata = (metadata, baseUrl) => {
  return metadata.map((line) => {
    const attributes = {};
    line.match(/([A-Z\-]+)="(.*?)"/g)?.forEach((attr) => {
      const [key, value] = attr.split('=');
      attributes[key] = value.replace(/"/g, ''); // Çift tırnakları kaldır
    });

    // Eğer URI varsa tam URL'ye dönüştür
    if (attributes.URI) {
      attributes.URI = new URL(attributes.URI, baseUrl).href;
    }

    return attributes;
  });
};

// DASH manifesti ayrıştırma
const parseDASHManifest = async (manifestContent) => {
  try {
    const parser = new xml2js.Parser();
    const result = await parser.parseStringPromise(manifestContent);
    return result;
  } catch (error) {
    throw new Error(`DASH manifest ayrıştırılamadı: ${error.message}`);
  }
};

// Manifest analizi
export const analyzeManifest = async (url) => {
  try {
    const manifestContent = await fetchManifest(url);
    const baseUrl = url.substring(0, url.lastIndexOf('/') + 1);

    // Manifest türünü belirle
    const manifestType = determineManifestType(manifestContent);

    if (manifestType === 'HLS') {
      const lines = manifestContent.split('\n');
      const metadataLines = lines.filter((line) => line.startsWith('#EXT-X-MEDIA'));
      const streamLines = lines.filter((line) => line.startsWith('#EXT-X-STREAM-INF') || !line.startsWith('#'));

      const hlsMetadata = parseHLSMetadata(metadataLines, baseUrl);
      const hlsStreams = parseHLSStreams(streamLines, baseUrl);

      console.log('HLS Metadata:', hlsMetadata);
      console.log('HLS Streams:', hlsStreams);

      return {type: 'HLS', metadata: hlsMetadata, streams: hlsStreams};
    } else if (manifestType === 'DASH') {
      const dashData = await parseDASHManifest(manifestContent);
      console.log('DASH Manifest:', dashData);

      return {type: 'DASH', manifest: dashData};
    } else {
      console.error('Desteklenmeyen manifest türü.');
      return {type: 'UNKNOWN', content: manifestContent};
    }
  } catch (error) {
    console.error('Hata:', error.message);
    throw error;
  }
};

