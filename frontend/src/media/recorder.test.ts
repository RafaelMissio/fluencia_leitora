import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createRecorder, isMediaRecorderSupported, pickSupportedMimeType } from './recorder'

/** Fake mínimo de `MediaRecorder` (jsdom não implementa a API - spec.md Edge Cases). */
class FakeMediaRecorder {
  static supportedTypes: string[] = []
  static isTypeSupportedCalls: string[] = []

  static isTypeSupported(mimeType: string): boolean {
    FakeMediaRecorder.isTypeSupportedCalls.push(mimeType)
    return FakeMediaRecorder.supportedTypes.includes(mimeType)
  }

  ondataavailable: ((event: { data: Blob }) => void) | null = null
  onstop: (() => void) | null = null
  mimeType: string
  calls: string[] = []

  constructor(_stream: MediaStream, options?: { mimeType?: string }) {
    this.mimeType = options?.mimeType ?? ''
  }

  start() {
    this.calls.push('start')
    this.ondataavailable?.({ data: new Blob(['pedaco']) })
  }

  pause() {
    this.calls.push('pause')
  }

  resume() {
    this.calls.push('resume')
  }

  stop() {
    this.calls.push('stop')
    this.onstop?.()
  }
}

const FAKE_STREAM = {} as MediaStream

describe('isMediaRecorderSupported', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns false when window.MediaRecorder is undefined', () => {
    vi.stubGlobal('MediaRecorder', undefined)

    expect(isMediaRecorderSupported()).toBe(false)
  })

  it('returns true when window.MediaRecorder is defined', () => {
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder)

    expect(isMediaRecorderSupported()).toBe(true)
  })
})

describe('pickSupportedMimeType', () => {
  beforeEach(() => {
    FakeMediaRecorder.isTypeSupportedCalls = []
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('respects the order webm/opus -> ogg/opus -> mp4, returning the first supported one', () => {
    FakeMediaRecorder.supportedTypes = ['audio/ogg;codecs=opus', 'audio/mp4']
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder)

    expect(pickSupportedMimeType()).toBe('audio/ogg;codecs=opus')
    expect(FakeMediaRecorder.isTypeSupportedCalls).toEqual(['audio/webm;codecs=opus', 'audio/ogg;codecs=opus'])
  })

  it('returns audio/webm;codecs=opus when it is the supported one, without checking the others', () => {
    FakeMediaRecorder.supportedTypes = ['audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/mp4']
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder)

    expect(pickSupportedMimeType()).toBe('audio/webm;codecs=opus')
  })

  it('returns null when none of the 3 MIME types is supported', () => {
    FakeMediaRecorder.supportedTypes = []
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder)

    expect(pickSupportedMimeType()).toBeNull()
  })

  it('returns null when window.MediaRecorder is undefined', () => {
    vi.stubGlobal('MediaRecorder', undefined)

    expect(pickSupportedMimeType()).toBeNull()
  })
})

describe('createRecorder', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('starts, pauses, resumes and stops, resolving with a Blob of the chosen mimeType', async () => {
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder)

    const handle = createRecorder(FAKE_STREAM, 'audio/webm;codecs=opus')
    handle.start()
    handle.pause()
    handle.resume()
    const blob = await handle.stop()

    expect(blob).toBeInstanceOf(Blob)
    expect(blob.type).toBe('audio/webm;codecs=opus')
  })
})
