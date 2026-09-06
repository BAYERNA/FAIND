import { ConfigService } from '@nestjs/config'
import { Test, TestingModule } from '@nestjs/testing'
import { getRepositoryToken } from '@nestjs/typeorm'
import { AlertsGateway } from './alerts.gateway'
import { IncidentAssignment } from './entities/incident-assignment.entity'

// 전체 점검(security audit)에서 발견한 결함의 회귀 방지 테스트: WebSocket 'join'이 JWT만
// 있으면 임의의 incidentId 룸에 아무나 들어갈 수 있던 문제. RESPONDER는 배정된 출동만,
// COMMANDER/ADMIN은 배정 무관 전체 접근(Java backend IncidentController와 동일한 경계).
describe('AlertsGateway', () => {
  let gateway: AlertsGateway
  let assignmentRepository: { findOne: jest.Mock }

  function makeClient(user: { userId: string; role: string } | undefined) {
    return {
      id: 'socket-1',
      data: { user },
      join: jest.fn(),
      emit: jest.fn(),
    } as any
  }

  beforeEach(async () => {
    assignmentRepository = { findOne: jest.fn() }

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AlertsGateway,
        { provide: ConfigService, useValue: { getOrThrow: jest.fn() } },
        { provide: getRepositoryToken(IncidentAssignment), useValue: assignmentRepository },
      ],
    }).compile()

    gateway = module.get(AlertsGateway)
  })

  it('배정되지 않은 RESPONDER는 join이 거부되고 방에 들어가지 않는다', async () => {
    assignmentRepository.findOne.mockResolvedValue(null)
    const client = makeClient({ userId: 'resp-1', role: 'RESPONDER' })

    await gateway.handleJoin(client, 'incident-1')

    expect(client.join).not.toHaveBeenCalled()
    expect(client.emit).toHaveBeenCalledWith('join:denied', expect.objectContaining({ incidentId: 'incident-1' }))
  })

  it('배정된 RESPONDER는 자기 출동 방에 join할 수 있다', async () => {
    assignmentRepository.findOne.mockResolvedValue({ assignmentId: 'a1', incidentId: 'incident-1', userId: 'resp-1' })
    const client = makeClient({ userId: 'resp-1', role: 'RESPONDER' })

    await gateway.handleJoin(client, 'incident-1')

    expect(client.join).toHaveBeenCalledWith('incident:incident-1')
    expect(client.emit).not.toHaveBeenCalledWith('join:denied', expect.anything())
  })

  it('COMMANDER는 배정 여부와 무관하게 어떤 출동 방이든 join할 수 있다', async () => {
    const client = makeClient({ userId: 'cmd-1', role: 'COMMANDER' })

    await gateway.handleJoin(client, 'incident-1')

    expect(assignmentRepository.findOne).not.toHaveBeenCalled()
    expect(client.join).toHaveBeenCalledWith('incident:incident-1')
  })

  it('ADMIN도 배정 여부와 무관하게 join할 수 있다', async () => {
    const client = makeClient({ userId: 'admin-1', role: 'ADMIN' })

    await gateway.handleJoin(client, 'incident-1')

    expect(assignmentRepository.findOne).not.toHaveBeenCalled()
    expect(client.join).toHaveBeenCalledWith('incident:incident-1')
  })
})
