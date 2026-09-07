export interface AuthenticatedUser {
  userId: string;
  badgeNumber: string;
  role: 'ADMIN' | 'COMMANDER' | 'RESPONDER';
}
