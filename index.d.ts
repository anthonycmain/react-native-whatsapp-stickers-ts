declare module 'react-native-whatsapp-stickers' {

  interface RNWhatsAppStickersModuleStatic {

    isWhatsAppAvailable: () => Promise<boolean>;

    // Android
    send(identifier: string, stickerPackName: string): Promise<string>;

    // iOS
    send(): Promise<string>;
    createStickerPack(config: object): Promise<string>;
    addSticker(fileName: string, emojis: Array<String>): Promise<string>;

  }

  const RNWhatsAppStickersModule: RNWhatsAppStickersModuleStatic;

  export default RNWhatsAppStickersModule;

}