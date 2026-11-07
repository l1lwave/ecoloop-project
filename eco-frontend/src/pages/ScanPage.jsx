import { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { BrowserMultiFormatReader } from '@zxing/browser'
import { DecodeHintType } from '@zxing/library'
import { identify } from '../api/scanApi'
import { deposit }  from '../api/rewardApi'

const DEMO_BARCODES = [
  { label: 'Spa Blauw 1.5L',         code: '8718906257894' },
  { label: 'Coca-Cola Regular 1.5L', code: '5449000000996' },
  { label: 'Heineken Bier 330ml',    code: '8710522950012' },
  { label: 'Red Bull 250ml',         code: '5000327009347' },
]

const HINTS = new Map([[DecodeHintType.TRY_HARDER, true]])

export default function ScanPage() {
  const navigate     = useNavigate()
  const { t, i18n } = useTranslation()

  const [barcode,   setBarcode]   = useState('')
  const [result,    setResult]    = useState(null)
  const [loading,   setLoading]   = useState(false)
  const [deposited, setDeposited] = useState(null)
  const [error,     setError]     = useState('')
  const [scanning,  setScanning]  = useState(false)
  const [camError,  setCamError]  = useState('')
  const [torchOn,        setTorchOn]        = useState(false)
  const [torchSupported, setTorchSupported] = useState(false)

  const videoRef  = useRef(null)
  const readerRef = useRef(null)
  const doneRef   = useRef(false)
  const trackRef  = useRef(null)

  // ── camera ────────────────────────────────────────────────────────────────

  const stopCamera = useCallback(() => {
    if (readerRef.current) {
      try { readerRef.current.stopAsyncDecode?.() } catch (_) {}
      readerRef.current = null
    }
    if (videoRef.current?.srcObject) {
      videoRef.current.srcObject.getTracks().forEach(t => t.stop())
      videoRef.current.srcObject = null
    }
    trackRef.current = null
    setTorchSupported(false)
    setTorchOn(false)
    setScanning(false)
    doneRef.current = false
  }, [])

  const startCamera = useCallback(async () => {
    setCamError('')
    setError('')
    doneRef.current = false
    try {
      const reader = new BrowserMultiFormatReader(HINTS)
      readerRef.current = reader
      setScanning(true)
      await reader.decodeFromConstraints(
        { video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } } },
        videoRef.current,
        (res, _err, controls) => {
          if (res && !doneRef.current) {
            doneRef.current = true
            controls.stop()
            readerRef.current = null
            setScanning(false)
            handleScan(res.getText())
          }
        }
      )
    } catch (e) {
      setCamError(e?.name === 'NotAllowedError' ? t('scan.camDenied') : t('scan.noCam'))
      setScanning(false)
      readerRef.current = null
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [t])

  useEffect(() => () => stopCamera(), [stopCamera])

  // detect torch support once the camera stream actually attaches to the video element
  useEffect(() => {
    if (!scanning) return
    let cancelled = false
    let raf

    const findTrack = () => {
      const track = videoRef.current?.srcObject?.getVideoTracks?.()[0]
      if (track) {
        trackRef.current = track
        const caps = track.getCapabilities?.() ?? {}
        if (!cancelled) setTorchSupported(!!caps.torch)
      } else if (!cancelled) {
        raf = requestAnimationFrame(findTrack)
      }
    }
    findTrack()

    return () => {
      cancelled = true
      if (raf) cancelAnimationFrame(raf)
    }
  }, [scanning])

  const toggleTorch = useCallback(async () => {
    const track = trackRef.current
    if (!track) return
    const next = !torchOn
    try {
      await track.applyConstraints({ advanced: [{ torch: next }] })
      setTorchOn(next)
    } catch (e) {
      console.warn('[Scan] torch toggle failed:', e)
    }
  }, [torchOn])

  // ── API ───────────────────────────────────────────────────────────────────

  const handleScan = useCallback(async (code) => {
    const target = (code ?? barcode).trim()
    if (!target) return
    setError('')
    setResult(null)
    setDeposited(null)
    setLoading(true)
    try {
      const { data } = await identify(target, i18n.language)
      setResult(data)
    } catch (e) {
      const s = e?.response?.status
      if      (s === 400) setError(t('scan.invalidBarcode'))
      else if (s === 404) setError(t('scan.productNotFound'))
      else if (s === 500) setError(t('scan.serverError'))
      else                setError(t('scan.notFound'))
    } finally {
      setLoading(false)
    }
  }, [barcode, i18n.language, t])

  const handleDeposit = async () => {
    if (!result) return
    setLoading(true)
    try {
      const { data } = await deposit({ barcode: result.barcode, materialType: result.materialType })
      setDeposited(data)
    } catch (e) {
      // Map known backend refusal codes to specific messages.
      // Raw server text and status codes are never shown to the user.
      const code = e?.response?.data?.error
      if      (code === 'NOT_RETURNABLE')      setError(t('scan.notReturnable'))
      else if (code === 'DEPOSIT_UNAVAILABLE') setError(t('scan.depositUnavailable'))
      else                                     setError(t('scan.depositFailed'))
    } finally {
      setLoading(false)
    }
  }

  const reset = () => { setResult(null); setDeposited(null); setBarcode(''); setError('') }

  // ── render ────────────────────────────────────────────────────────────────

  return (
    <div className="h-screen md:h-[calc(100vh-3.5rem)] flex flex-col bg-gray-950 max-w-md mx-auto overflow-hidden relative">

      {/* ── Centred error toast (visible over camera AND form) ── */}
      {error && (
        <div className="absolute inset-x-4 top-1/2 -translate-y-1/2 z-50 flex flex-col items-center">
          <div className="w-full bg-white rounded-3xl shadow-2xl px-6 py-6 flex flex-col items-center gap-3 border border-red-100">
            <div className="w-14 h-14 rounded-full bg-red-100 flex items-center justify-center">
              <svg className="w-7 h-7 text-red-500" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
              </svg>
            </div>
            <p className="text-gray-800 text-base font-semibold text-center leading-snug">{error}</p>
            <button onClick={() => setError('')}
              className="mt-1 min-h-[44px] w-full bg-eco-600 text-white rounded-xl py-2.5 text-sm font-semibold">
              {t('common.tryAgain')}
            </button>
          </div>
        </div>
      )}

      {/* ── Camera area — fixed height so the panel below always has room ── */}
      <div className="h-[42vh] flex-shrink-0 relative flex flex-col items-center justify-center overflow-hidden bg-gray-950">

        {/* back button */}
        <button onClick={() => navigate(-1)}
          className="absolute top-10 left-4 z-10 flex min-h-[44px] items-center gap-1 text-white bg-black/50 backdrop-blur-sm rounded-full px-3 py-1.5 text-sm font-medium">
          ← {t('common.back')}
        </button>

        {/* torch toggle — only shown once the active camera track reports torch support */}
        {scanning && torchSupported && (
          <button onClick={toggleTorch} aria-label={t('scan.torch')}
            className={`absolute top-10 right-4 z-10 w-11 h-11 flex items-center justify-center rounded-full backdrop-blur-sm transition-colors ${
              torchOn ? 'bg-yellow-400 text-gray-900' : 'bg-black/50 text-white'
            }`}>
            <svg className="w-4 h-4" fill={torchOn ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M13 2L4.5 13.5H11L10 22l8.5-11.5H12L13 2z"/>
            </svg>
          </button>
        )}

        {/* live video */}
        <video
          ref={videoRef}
          autoPlay muted playsInline
          className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-300 ${scanning ? 'opacity-100' : 'opacity-0'}`}
        />

        {/* scanning frame */}
        {scanning && (
          <div className="relative z-10 w-56 h-56 pointer-events-none">
            <div className="absolute inset-0 border-2 border-eco-400/50 rounded-2xl" />
            <div className="absolute top-0 left-0  w-8 h-8 border-t-4 border-l-4 border-eco-400 rounded-tl-2xl" />
            <div className="absolute top-0 right-0 w-8 h-8 border-t-4 border-r-4 border-eco-400 rounded-tr-2xl" />
            <div className="absolute bottom-0 left-0  w-8 h-8 border-b-4 border-l-4 border-eco-400 rounded-bl-2xl" />
            <div className="absolute bottom-0 right-0 w-8 h-8 border-b-4 border-r-4 border-eco-400 rounded-br-2xl" />
            <div className="absolute left-2 right-2 h-0.5 bg-eco-400 animate-scan-line" />
          </div>
        )}

        {/* idle icon — perfectly centred because parent is flex items-center justify-center */}
        {!scanning && (
          <div className="flex flex-col items-center gap-4 z-10">
            <div className="w-24 h-24 rounded-full bg-eco-600/20 flex items-center justify-center ring-1 ring-eco-600/30">
              <svg className="w-12 h-12 text-eco-400" fill="none" stroke="currentColor" strokeWidth={1.4} viewBox="0 0 24 24">
                <rect x="3"  y="3"  width="5" height="5" rx="0.5"/>
                <rect x="16" y="3"  width="5" height="5" rx="0.5"/>
                <rect x="3"  y="16" width="5" height="5" rx="0.5"/>
                <rect x="10" y="3"  width="2" height="2"/>
                <rect x="10" y="7"  width="2" height="2"/>
                <rect x="14" y="10" width="2" height="2"/>
                <rect x="10" y="14" width="2" height="2"/>
                <rect x="14" y="14" width="2" height="2"/>
                <rect x="10" y="19" width="2" height="2"/>
                <rect x="16" y="16" width="5" height="2"/>
                <rect x="19" y="19" width="2" height="2"/>
              </svg>
            </div>
            {camError && (
              <p className="text-red-400 text-xs text-center px-8 max-w-xs">{camError}</p>
            )}
          </div>
        )}

        {/* start / stop button */}
        <button
          onClick={scanning ? stopCamera : startCamera}
          className={`absolute bottom-6 z-10 px-7 py-3 rounded-full text-sm font-semibold shadow-xl transition-colors
            ${scanning ? 'bg-red-500 text-white' : 'bg-eco-500 text-white'}`}>
          {scanning ? t('scan.stopCam') : t('scan.startCam')}
        </button>
      </div>

      {/* ── Bottom white panel — takes rest of space, scrollable, clears nav bar ── */}
      <div className="flex-1 bg-white rounded-t-3xl shadow-2xl overflow-y-auto">
        <div className="p-6 pb-24">

          {deposited ? (
            <div className="text-center py-2">
              <div className="text-4xl mb-2">🎉</div>
              <h3 className="text-lg font-bold text-eco-700">{t('scan.submitted')}</h3>
              <p className="text-gray-500 text-sm mt-1">+{deposited.pointsEarned} {t('scan.pointsEarned')}</p>
              {deposited.newBadges?.length > 0 && (
                <p className="text-eco-600 text-sm font-semibold mt-1">
                  {t('scan.newBadge')} {deposited.newBadges.map(id => t(`badges.${id}.name`, { defaultValue: id })).join(', ')}
                </p>
              )}
              <button onClick={reset}
                className="mt-4 w-full bg-eco-600 text-white rounded-xl py-3 text-sm font-semibold">
                {t('scan.scanAgain')}
              </button>
            </div>

          ) : result ? (
            <div>
              {/* Product name + brand */}
              <h3 className="text-lg font-bold text-gray-900 leading-tight break-words">{result.productName}</h3>
              {result.brand && (
                <p className="text-sm text-eco-700 font-medium mt-0.5">{result.brand}</p>
              )}

              {/* Recyclable badge + material */}
              <div className="flex items-center gap-2 mt-2">
                <span className={`text-sm font-medium ${result.recyclable ? 'text-eco-600' : 'text-red-500'}`}>
                  {result.recyclable ? `✅ ${t('scan.recyclable')}` : `❌ ${t('scan.notRecyclable')}`}
                </span>
                <span className="text-gray-300">·</span>
                <span className="text-sm text-gray-400">{result.materialType}</span>
                {result.depositValue > 0 && (
                  <>
                    <span className="text-gray-300">·</span>
                    <span className="text-sm font-semibold text-eco-600">€{result.depositValue.toFixed(2)}</span>
                  </>
                )}
              </div>

              {/* Recyclable but not deposit-eligible: explain why there is no Returned button */}
              {result.recyclable && !(result.depositValue > 0) && (
                <p className="text-xs text-gray-500 mt-2">{t('scan.notReturnableInfo')}</p>
              )}

              {/* Instructions */}
              {result.instructions && (
                <p className="text-sm text-gray-500 mt-2 bg-gray-50 rounded-xl p-3 leading-relaxed break-words">
                  {result.instructions}
                </p>
              )}

              {/* Action buttons */}
              <div className="flex gap-3 mt-4">
                {/* Opens in a new tab instead of navigating this one away — react-router's
                    navigate() would unmount ScanPage and lose the scanned result/state,
                    making it impossible to come back and press "Returned" afterward. */}
                <button onClick={() => window.open('/map', '_blank', 'noopener,noreferrer')}
                  className="flex-1 min-h-[44px] border border-eco-300 text-eco-700 rounded-xl py-2.5 text-sm font-medium">
                  {t('scan.findPoint')}
                </button>
                {/* Returned button only for products that are really deposit-eligible */}
                {result.depositValue > 0 && (
                  <button onClick={handleDeposit} disabled={loading}
                    className="flex-1 min-h-[44px] bg-eco-600 text-white rounded-xl py-2.5 text-sm font-semibold disabled:opacity-50">
                    {loading ? t('common.loading') : t('scan.submitBtn')}
                  </button>
                )}
              </div>
              <button onClick={reset} className="mt-3 min-h-[44px] w-full text-gray-400 text-sm py-1">
                {t('scan.scanAgain')}
              </button>
            </div>

          ) : (
            <div>
              <p className="text-center text-gray-400 text-sm mb-4">{t('scan.manualHint')}</p>
              <div className="flex gap-2 mb-3">
                <input
                  value={barcode}
                  onChange={(e) => setBarcode(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleScan()}
                  placeholder={t('scan.placeholder')}
                  inputMode="numeric"
                  maxLength={14}
                  className="min-h-[44px] flex-1 border border-gray-200 rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-eco-400"
                />
                <button onClick={() => handleScan()} disabled={!barcode || loading}
                  className="min-h-[44px] bg-eco-600 text-white rounded-xl px-4 py-2.5 text-sm font-semibold disabled:opacity-50 min-w-[60px] flex items-center justify-center">
                  {loading
                    ? <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    : t('scan.scanBtn')}
                </button>
              </div>
              <div className="grid grid-cols-2 gap-2">
                {DEMO_BARCODES.map((b) => (
                  <button key={b.code} onClick={() => handleScan(b.code)}
                    className="min-h-[44px] text-sm border border-eco-200 bg-eco-50 text-eco-700 rounded-xl px-3 py-2 text-left truncate">
                    {b.label}
                  </button>
                ))}
              </div>
            </div>
          )}

        </div>
      </div>
    </div>
  )
}
