import { Ionicons } from '@expo/vector-icons';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { WebView } from 'react-native-webview';

import { LoadingState } from '@/components/loading-state';
import { colors } from '@/constants/theme';

export type KakaoLoginSheetProps = { url?: string; onClose: () => void; onNavigate: (url: string) => boolean; onHttpError: () => void };

export function KakaoLoginSheet({ url, onClose, onNavigate, onHttpError }: KakaoLoginSheetProps) {
  return <Modal animationType="slide" presentationStyle="pageSheet" visible={Boolean(url)} onRequestClose={onClose}>
    <SafeAreaView style={styles.sheet}>
      <View style={styles.header}><Pressable accessibilityRole="button" accessibilityLabel="카카오 로그인 닫기" onPress={onClose} style={styles.close}><Ionicons name="close" size={24} color={colors.ink900}/></Pressable><Text style={styles.title}>카카오 로그인</Text><View style={styles.close}/></View>
      {url ? <WebView source={{ uri: url }} sharedCookiesEnabled startInLoadingState renderLoading={() => <View style={styles.loading}><LoadingState compact inline label="카카오 로그인을 불러오고 있어요"/></View>} onShouldStartLoadWithRequest={request => onNavigate(request.url)} onHttpError={onHttpError}/> : null}
    </SafeAreaView>
  </Modal>;
}

const styles = StyleSheet.create({ sheet: { flex: 1, backgroundColor: colors.white }, header: { height: 54, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: 1, borderBottomColor: colors.line }, close: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' }, title: { color: colors.ink900, fontSize: 15, fontWeight: '900' }, loading: { ...StyleSheet.absoluteFillObject, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.white } });
