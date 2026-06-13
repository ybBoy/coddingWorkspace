export type ReadingStatus = 'TO_READ' | 'READING' | 'READ';

export interface Book {
  id: string;
  title: string;
  author: string;
  status: ReadingStatus;
  remark: string;
  createdAt: number;
}

export interface BookInput {
  title: string;
  author: string;
  status: ReadingStatus;
  remark: string;
}

export const STATUS_LABELS: Record<ReadingStatus, string> = {
  TO_READ: '想读',
  READING: '在读',
  READ: '已读'
};
