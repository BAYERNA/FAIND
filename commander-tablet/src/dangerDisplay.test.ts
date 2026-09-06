import { describe, expect, it } from 'vitest'
import { DANGER_CLASS, DANGER_LABEL, DANGER_POLL_INTERVAL_MS, SPREAD_LABEL } from './dangerDisplay'

// LiveCameraPanel·DroneReconCard 둘 다 ai-server의 dangerLevel/spreadDirection 문자열을 그대로
// 이 맵의 키로 조회한다 — 키 하나만 빠져도 배지가 조용히 "undefined"로 렌더링된다.
describe('dangerDisplay', () => {
  const DANGER_LEVELS = ['SAFE', 'WARNING', 'DANGER', 'CRITICAL']
  const SPREAD_DIRECTIONS = ['UP', 'DOWN', 'LEFT', 'RIGHT']

  it('4단계 위험도 전부에 표시 라벨이 있다', () => {
    for (const level of DANGER_LEVELS) {
      expect(DANGER_LABEL[level]).toBeTruthy()
    }
  })

  it('4단계 위험도 전부에 CSS 클래스가 있다', () => {
    for (const level of DANGER_LEVELS) {
      expect(DANGER_CLASS[level]).toBeTruthy()
    }
  })

  it('4방향 확산 전부에 표시 라벨이 있다', () => {
    for (const direction of SPREAD_DIRECTIONS) {
      expect(SPREAD_LABEL[direction]).toBeTruthy()
    }
  })

  it('폴링 주기는 0보다 큰 양수다', () => {
    expect(DANGER_POLL_INTERVAL_MS).toBeGreaterThan(0)
  })
})
