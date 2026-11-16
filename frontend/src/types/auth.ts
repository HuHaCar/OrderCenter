export interface AuthUser {
  id: number
  username: string
  displayName: string
  role: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface AuthTokenResult {
  accessToken: string
  expiresIn: number
  user: AuthUser
}
