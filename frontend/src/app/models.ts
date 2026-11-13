export interface UserModel {
  id: number;
  email: string;
  nom: string;
  prenom: string;
  tel: string;
  roles: string[];
}

export interface RoleModel {
  id: number;
  libelle: string;
}

export interface Client {
  id?: number;
  nom: string;
  tel: string;
  email: string;
  adresse: string;
}

export interface ProjectModel {
  id?: number;
  libelle: string;
  budget: number;
  duree: number;
  description: string;
  client?: Pick<Client, 'id' | 'nom'>;
  clientId?: number;
  imageUrl?: string | null;
}

export type InvoiceStatus = 'UNPAID' | 'PAID';

export interface InvoiceModel {
  id: number;
  numero: string;
  clientId: number;
  clientName: string;
  projectId: number | null;
  projectLibelle: string | null;
  status: InvoiceStatus;
  issueDate: string;
  dueDate: string | null;
  paidDate: string | null;
  amount: number;
  taxRate: number;
  totalAmount: number;
  paymentMethod: string | null;
  notes: string | null;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InvoiceRequest {
  numero: string | null;
  clientId: number;
  projectId: number | null;
  issueDate: string;
  dueDate: string | null;
  amount: number;
  taxRate: number;
  paymentMethod: string | null;
  notes: string | null;
}

export interface InvoiceStats {
  totalInvoiced: number;
  totalPaid: number;
  totalUnpaid: number;
  paidCount: number;
  unpaidCount: number;
  overdueCount: number;
}

export interface InvoiceFileModel {
  id: number;
  fileName: string;
  contentType: string;
  size: number;
  createdAt: string;
  downloadUrl: string;
}

export type ProjectRole = 'MANAGER' | 'DEVELOPER' | 'DESIGNER' | 'TESTER';

export interface ProjectMemberModel {
  id: number;
  projectId: number;
  userId: number;
  userName: string;
  userEmail: string;
  projectRole: ProjectRole;
  joinedAt: string;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface TaskModel {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  startDate: string | null;
  dueDate: string | null;
  projectId: number;
  projectLibelle: string;
  assignedUserId: number | null;
  assignedUserName: string | null;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TaskRequest {
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  startDate: string | null;
  dueDate: string | null;
  projectId: number;
  assignedUserId: number | null;
}

export interface TaskStats {
  total: number;
  todo: number;
  inProgress: number;
  done: number;
  overdue: number;
  urgent: number;
  totalHours: number;
}

export type MilestoneStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED';

export interface MilestoneModel {
  id: number;
  title: string;
  description: string | null;
  dueDate: string | null;
  status: MilestoneStatus;
  projectId: number;
  projectLibelle: string;
  overdue: boolean;
  createdAt: string;
}

export interface MilestoneRequest {
  title: string;
  description: string | null;
  dueDate: string | null;
  status: MilestoneStatus;
  projectId: number;
}

export interface TaskCommentModel {
  id: number;
  taskId: number;
  authorId: number;
  authorName: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface TimeEntryModel {
  id: number;
  taskId: number;
  userId: number;
  userName: string;
  hours: number;
  description: string | null;
  entryDate: string;
  createdAt: string;
}

export interface TimeEntryRequest {
  hours: number;
  description: string | null;
  entryDate: string;
}
