import { IsIn, IsOptional, IsString, MaxLength } from 'class-validator';
import { InfoCategory, StatusTag } from '../entities/alert.entity';

// FR-18: 선발대 진입정보 공유. 자유텍스트 대신 위치·상태 태그를 우선하고, message는 보조 수단.
export class CreateEntryInfoDto {
  @IsIn(['ENTRY', 'HAZARD'])
  infoCategory: InfoCategory;

  @IsString()
  @MaxLength(50)
  locationLabel: string;

  @IsIn(['PASSABLE', 'BLOCKED', 'DANGER'])
  statusTag: StatusTag;

  @IsOptional()
  @IsString()
  @MaxLength(2000)
  message?: string;
}
