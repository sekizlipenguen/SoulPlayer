declare module '@sekizlipenguen/react-native-soul-player' {
    import * as React from 'react';
    import {StyleProp, ViewStyle} from 'react-native';

    export interface SoulPlayerProps {
        videoUrl: string; // Video dosyasının URL'si
        videoType?: 'mp4' | 'm3u8'; // Video formatı
        paused?: boolean; // Video duraklatıldı mı?
        onLoadStart?: () => void; // Video yüklenmeye başladığında çağrılır
        onError?: (error: any) => void; // Video hata aldığında çağrılır
        onEnd?: () => void; // Video bittiğinde çağrılır
        onSeek?: (time: number) => void; // Seek işlemi gerçekleştiğinde çağrılır
        onProgress?: (progress: { currentTime: number; duration: number }) => void; // Video ilerleme durumunu döner
        onLoad?: (data: any) => void; // Video yüklendiğinde çağrılır
        onMuteToggle?: (muted: boolean) => void; // Ses durumu değiştiğinde çağrılır
        onQualityChange?: (quality: any) => void; // Kalite değişimi olduğunda çağrılır
        onFullScreen?: (status: boolean) => void; // Tam ekran durumu değiştiğinde çağrılır
        onPlay?: (time: number) => void; // Video oynatıldığında çağrılır
        onPause?: (time: number) => void; // Video duraklatıldığında çağrılır
        onBackButton?: () => void; // Geri butonuna tıklandığında çağrılır
        showBackButton?: boolean; // Geri butonu gösterilsin mi?
        style?: StyleProp<ViewStyle>; // Video player stil tanımlamaları
    }

    export default class SoulPlayer extends React.Component<SoulPlayerProps> {
    }
}
