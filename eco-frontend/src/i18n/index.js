import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from './locales/en.json'
import nl from './locales/nl.json'
import ua from './locales/ua.json'

i18n.use(initReactI18next).init({
  resources: { en: { translation: en }, nl: { translation: nl }, ua: { translation: ua } },
  lng: localStorage.getItem('eco_lang') || 'nl',
  fallbackLng: 'nl',
  interpolation: { escapeValue: false },
})

export default i18n
