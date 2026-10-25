export interface PaginatedResponse<T> {
  content: T[],
  pageable: Pageable,
  totalElements: number,
  totalPages: number,
  last: boolean,
  size: number,
  number: number,
  sort: Sort,
  first: boolean,
  numberOfElements: number,
  empty: boolean
}

interface Pageable {
  pageNumber: number,
  pageSize: number,
  sort: Sort,
  offset: number,
  paged: boolean,
  unpaged: boolean
}

interface Sort {
  sorted: boolean,
  empty: boolean,
  unsorted: boolean
}

interface ErrorField {
  field: string,
  detail: string
}

export interface ApiErrorResponse {
  statusCode: number,
  path: string,
  message: string,
  timestamp: Date,
  errors?: ErrorField[]
}
