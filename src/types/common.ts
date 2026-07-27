export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  message: string;
  status: number;
  timestamp?: string;
}

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type AssessmentStatus = 'UPCOMING' | 'IN_PROGRESS' | 'COMPLETED' | 'EXPIRED';

export type QuestionType =
  | 'CODING'
  | 'MCQ_SINGLE'
  | 'MCQ_MULTI'
  | 'SUBJECTIVE'
  | 'READING_COMPREHENSION'
  | 'FILE_UPLOAD';

export type SubmissionStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'COMPILATION_ERROR'
  | 'RUNTIME_ERROR'
  | 'PARTIALLY_ACCEPTED';
