import { Children, useRef, useState } from 'react'
import { panelDomId, tabDomId } from './profileTabsConfig'

const DRAG_START_PX = 8     // movement needed before a gesture counts as a drag
const EDGE_RESISTANCE = 0.3 // past the first or last window the strip moves only this fraction

// Horizontal swipe container for the profile windows. Each child is one window.
// Uses native pointer events only. Vertical gestures are left to the browser
// (touch-pan-y), so normal page scrolling keeps working.
export default function SwipeableWindows({ baseId, activeIndex, onChange, children }) {
  const panels      = Children.toArray(children)
  const lastIndex   = panels.length - 1
  const containerRef = useRef(null)
  const gestureRef   = useRef(null) // { pointerId, startX, startY, dx, width, mode }
  const movedRef     = useRef(false) // set after a horizontal drag to swallow the click that may follow
  const [dragOffset, setDragOffset] = useState(0)
  const [dragging, setDragging]     = useState(false)

  const handlePointerDown = (event) => {
    if (!event.isPrimary) return
    movedRef.current = false
    gestureRef.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      dx: 0,
      width: 0,
      mode: 'pending', // 'pending' | 'horizontal' | 'vertical'
    }
  }

  const handlePointerMove = (event) => {
    const gesture = gestureRef.current
    if (!gesture || gesture.pointerId !== event.pointerId || gesture.mode === 'vertical') return

    const dx = event.clientX - gesture.startX
    const dy = event.clientY - gesture.startY

    if (gesture.mode === 'pending') {
      if (Math.abs(dx) < DRAG_START_PX && Math.abs(dy) < DRAG_START_PX) return
      if (Math.abs(dy) > Math.abs(dx)) {
        gesture.mode = 'vertical'
        return
      }
      // Horizontal intent confirmed: lock the gesture and capture the pointer.
      gesture.mode  = 'horizontal'
      gesture.width = containerRef.current?.offsetWidth ?? 0
      movedRef.current = true
      event.currentTarget.setPointerCapture?.(event.pointerId)
      setDragging(true)
    }

    gesture.dx = dx
    const atEdge = (activeIndex === 0 && dx > 0) || (activeIndex === lastIndex && dx < 0)
    setDragOffset(atEdge ? dx * EDGE_RESISTANCE : dx)
  }

  const handlePointerEnd = (event) => {
    const gesture = gestureRef.current
    if (!gesture || gesture.pointerId !== event.pointerId) return
    gestureRef.current = null

    if (gesture.mode === 'horizontal') {
      // Snap to the nearest window based on how far the strip was dragged.
      const width  = gesture.width || 1
      const target = Math.min(lastIndex, Math.max(0, Math.round(activeIndex - gesture.dx / width)))
      setDragging(false)
      setDragOffset(0)
      if (target !== activeIndex) onChange(target)
    }
  }

  // After a horizontal drag, a click on a control under the pointer is not a real click.
  const handleClickCapture = (event) => {
    if (!movedRef.current) return
    movedRef.current = false
    event.stopPropagation()
    event.preventDefault()
  }

  const trackStyle = {
    transform: `translate3d(calc(${-activeIndex * 100}% + ${dragOffset}px), 0, 0)`,
  }

  return (
    <div
      ref={containerRef}
      className="mt-4 touch-pan-y overflow-hidden"
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
      onClickCapture={handleClickCapture}
    >
      <div
        className={`flex ${
          dragging ? '' : 'transition-transform duration-300 ease-out motion-reduce:transition-none'
        }`}
        style={trackStyle}
      >
        {panels.map((panel, index) => {
          const isActive = index === activeIndex
          return (
            <div
              key={index}
              id={panelDomId(baseId, index)}
              role="tabpanel"
              aria-labelledby={tabDomId(baseId, index)}
              aria-hidden={!isActive}
              // Inactive windows are made inert so their controls cannot be reached by keyboard.
              // Set as a DOM property because React 18 does not support the inert attribute.
              ref={(el) => { if (el) el.inert = !isActive }}
              className="w-full min-w-0 shrink-0"
            >
              {panel}
            </div>
          )
        })}
      </div>
    </div>
  )
}
