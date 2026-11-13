import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  ProjectModel,
  TaskCommentModel,
  TaskModel,
  TaskPriority,
  TaskRequest,
  TaskStats,
  TaskStatus,
  TimeEntryModel,
  UserModel,
} from '../../models';
import { AuthService } from '../../services/auth.service';
import { ErrorNotificationService } from '../../services/error-notification.service';
import { ProjectService } from '../../services/project.service';
import { TaskService } from '../../services/task.service';
import { UserService } from '../../services/user.service';

interface BoardColumn {
  status: TaskStatus;
  label: string;
  icon: string;
  accent: string;
}

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <section class="container-fluid py-4">
      <div class="row">
        <div class="col-xl-3 col-md-6 mb-4">
          <div class="card card-stats mb-0">
            <div class="card-body">
              <div class="row">
                <div class="col">
                  <h5 class="card-title text-uppercase text-muted mb-0">Tâches</h5>
                  <span class="h2 font-weight-bold mb-0">{{ stats.total }}</span>
                </div>
                <div class="col-auto">
                  <div class="icon icon-shape bg-primary text-white rounded-circle shadow">
                    <i class="ni ni-bullet-list-67"></i>
                  </div>
                </div>
              </div>
              <p class="mt-3 mb-0 text-muted text-sm">{{ stats.done }} terminée(s) · {{ stats.totalHours }} h pointées</p>
            </div>
          </div>
        </div>
        <div class="col-xl-3 col-md-6 mb-4">
          <div class="card card-stats mb-0">
            <div class="card-body">
              <div class="row">
                <div class="col">
                  <h5 class="card-title text-uppercase text-muted mb-0">En cours</h5>
                  <span class="h2 font-weight-bold mb-0 text-info">{{ stats.inProgress }}</span>
                </div>
                <div class="col-auto">
                  <div class="icon icon-shape bg-info text-white rounded-circle shadow">
                    <i class="ni ni-settings-gear-65"></i>
                  </div>
                </div>
              </div>
              <p class="mt-3 mb-0 text-muted text-sm">{{ stats.todo }} à faire</p>
            </div>
          </div>
        </div>
        <div class="col-xl-3 col-md-6 mb-4">
          <div class="card card-stats mb-0">
            <div class="card-body">
              <div class="row">
                <div class="col">
                  <h5 class="card-title text-uppercase text-muted mb-0">En retard</h5>
                  <span class="h2 font-weight-bold mb-0 text-danger">{{ stats.overdue }}</span>
                </div>
                <div class="col-auto">
                  <div class="icon icon-shape bg-danger text-white rounded-circle shadow">
                    <i class="ni ni-time-alarm"></i>
                  </div>
                </div>
              </div>
              <p class="mt-3 mb-0 text-muted text-sm">{{ stats.urgent }} urgente(s)</p>
            </div>
          </div>
        </div>
        <div class="col-xl-3 col-md-6 mb-4">
          <div class="card card-stats mb-0">
            <div class="card-body">
              <div class="row">
                <div class="col">
                  <h5 class="card-title text-uppercase text-muted mb-0">Terminées</h5>
                  <span class="h2 font-weight-bold mb-0 text-success">{{ stats.done }}</span>
                </div>
                <div class="col-auto">
                  <div class="icon icon-shape bg-success text-white rounded-circle shadow">
                    <i class="ni ni-check-bold"></i>
                  </div>
                </div>
              </div>
              <p class="mt-3 mb-0 text-muted text-sm">Tâches livrées</p>
            </div>
          </div>
        </div>
      </div>

      <div class="card shadow mb-4">
        <div class="card-header border-0">
          <div class="d-flex flex-wrap align-items-center justify-content-between">
            <h2 class="h3 mb-2 mb-md-0">{{ editingId ? 'Modifier la tâche' : 'Nouvelle tâche' }}</h2>
            <div class="d-flex flex-wrap align-items-center">
              <select class="form-control form-control-sm mr-2 mb-1" [(ngModel)]="filterProjectId" aria-label="Filtrer par projet">
                <option [ngValue]="null">Tous les projets</option>
                <option *ngFor="let project of projects" [ngValue]="project.id">{{ project.libelle }}</option>
              </select>
              <select class="form-control form-control-sm mr-2 mb-1" [(ngModel)]="filterAssigneeId" aria-label="Filtrer par collaborateur">
                <option [ngValue]="null">Tous les collaborateurs</option>
                <option *ngFor="let user of users" [ngValue]="user.id">{{ user.prenom }} {{ user.nom }}</option>
              </select>
              <select class="form-control form-control-sm mr-2 mb-1" [(ngModel)]="filterPriority" aria-label="Filtrer par priorité">
                <option value="ALL">Toutes priorités</option>
                <option value="LOW">Basse</option>
                <option value="MEDIUM">Moyenne</option>
                <option value="HIGH">Haute</option>
                <option value="URGENT">Urgente</option>
              </select>
              <input
                class="form-control form-control-sm mb-1"
                [(ngModel)]="search"
                placeholder="Rechercher…"
                aria-label="Rechercher une tâche"
              />
            </div>
          </div>
        </div>
        <div class="card-body">
          <form [formGroup]="taskForm" (ngSubmit)="submitTask()">
            <div class="row">
              <div class="col-md-6">
                <div class="form-group">
                  <label class="form-control-label" for="task-title">Titre</label>
                  <input id="task-title" class="form-control" formControlName="title" placeholder="Ex : Écran de connexion" />
                </div>
              </div>
              <div class="col-md-3">
                <div class="form-group">
                  <label class="form-control-label" for="task-project">Projet</label>
                  <select id="task-project" class="form-control" formControlName="projectId">
                    <option [ngValue]="null" disabled>Choisir un projet</option>
                    <option *ngFor="let project of projects" [ngValue]="project.id">{{ project.libelle }}</option>
                  </select>
                </div>
              </div>
              <div class="col-md-3">
                <div class="form-group">
                  <label class="form-control-label" for="task-assignee">Assignée à</label>
                  <select id="task-assignee" class="form-control" formControlName="assignedUserId">
                    <option [ngValue]="null">Non assignée</option>
                    <option *ngFor="let user of users" [ngValue]="user.id">{{ user.prenom }} {{ user.nom }}</option>
                  </select>
                </div>
              </div>
            </div>
            <div class="row">
              <div class="col-md-3">
                <div class="form-group">
                  <label class="form-control-label" for="task-status">Statut</label>
                  <select id="task-status" class="form-control" formControlName="status">
                    <option value="TODO">À faire</option>
                    <option value="IN_PROGRESS">En cours</option>
                    <option value="DONE">Terminée</option>
                  </select>
                </div>
              </div>
              <div class="col-md-3">
                <div class="form-group">
                  <label class="form-control-label" for="task-priority">Priorité</label>
                  <select id="task-priority" class="form-control" formControlName="priority">
                    <option value="LOW">Basse</option>
                    <option value="MEDIUM">Moyenne</option>
                    <option value="HIGH">Haute</option>
                    <option value="URGENT">Urgente</option>
                  </select>
                </div>
              </div>
              <div class="col-md-3">
                <div class="form-group">
                  <label class="form-control-label" for="task-start">Début</label>
                  <input id="task-start" class="form-control" type="date" formControlName="startDate" />
                </div>
              </div>
              <div class="col-md-3">
                <div class="form-group">
                  <label class="form-control-label" for="task-due">Échéance</label>
                  <input id="task-due" class="form-control" type="date" formControlName="dueDate" />
                </div>
              </div>
            </div>
            <div class="form-group">
              <label class="form-control-label" for="task-description">Description</label>
              <textarea id="task-description" class="form-control" rows="2" formControlName="description"></textarea>
            </div>
            <button type="submit" class="btn btn-primary" [disabled]="taskForm.invalid || saving">
              {{ saving ? 'Enregistrement…' : editingId ? 'Enregistrer' : 'Créer la tâche' }}
            </button>
            <button type="button" class="btn btn-link text-muted" *ngIf="editingId" (click)="resetTaskForm()">Annuler</button>
          </form>
        </div>
      </div>

      <div class="row">
        <div class="col-lg-4 mb-4" *ngFor="let column of columns">
          <div
            class="card shadow board-column"
            [attr.data-status]="column.status"
            (dragover)="allowDrop($event)"
            (drop)="dropOn(column.status)"
          >
            <div class="card-header border-0 d-flex align-items-center justify-content-between">
              <h3 class="h5 mb-0 d-flex align-items-center">
                <i class="ni {{ column.icon }} mr-2" [ngClass]="column.accent"></i>
                {{ column.label }}
              </h3>
              <span class="badge badge-pill badge-light">{{ tasksFor(column.status).length }}</span>
            </div>
            <div class="card-body board-body">
              <article
                *ngFor="let task of tasksFor(column.status)"
                class="task-card"
                [class.task-card--overdue]="task.overdue"
                [class.task-card--done]="task.status === 'DONE'"
                draggable="true"
                (dragstart)="startDrag(task)"
                (dragend)="endDrag()"
              >
                <div class="d-flex justify-content-between align-items-start">
                  <h4 class="h6 mb-1 task-title">{{ task.title }}</h4>
                  <span class="badge badge-pill" [ngClass]="priorityClass(task.priority)">
                    {{ priorityLabel(task.priority) }}
                  </span>
                </div>
                <p class="text-muted text-sm mb-2">
                  <i class="ni ni-collection mr-1"></i>{{ task.projectLibelle }}
                </p>
                <div class="d-flex flex-wrap text-sm text-muted mb-2">
                  <span class="mr-3">
                    <i class="ni ni-single-02 mr-1"></i>{{ task.assignedUserName ?? 'Non assignée' }}
                  </span>
                  <span [ngClass]="task.overdue ? 'text-danger font-weight-bold' : ''">
                    <i class="ni ni-calendar-grid-58 mr-1"></i>
                    {{ task.dueDate ? (task.dueDate | date: 'dd/MM/yyyy') : 'Sans échéance' }}
                  </span>
                </div>
                <div class="d-flex flex-wrap align-items-center">
                  <button type="button" class="btn btn-sm btn-outline-secondary mr-2 mb-1" (click)="toggleDetails(task)">
                    <i class="ni ni-chat-round mr-1"></i>
                    Détails
                    <span *ngIf="commentsFor(task.id).length">({{ commentsFor(task.id).length }})</span>
                  </button>
                  <button type="button" class="btn btn-sm btn-outline-primary mr-2 mb-1" (click)="editTask(task)">Modifier</button>
                  <button
                    *ngIf="task.status !== 'DONE'"
                    type="button"
                    class="btn btn-sm btn-outline-success mr-2 mb-1"
                    (click)="moveTask(task, 'DONE')"
                  >
                    Terminer
                  </button>
                  <button
                    *ngIf="isAdmin"
                    type="button"
                    class="btn btn-sm btn-outline-danger mb-1"
                    (click)="deleteTask(task)"
                  >
                    Supprimer
                  </button>
                </div>

                <div *ngIf="expandedTaskId === task.id" class="task-details mt-3">
                  <div class="d-flex justify-content-between align-items-center mb-2">
                    <h5 class="h6 mb-0">Suivi de la tâche</h5>
                    <button type="button" class="btn btn-sm btn-link text-muted p-0" (click)="collapseDetails()">Fermer</button>
                  </div>
                  <p *ngIf="task.description" class="text-sm text-muted">{{ task.description }}</p>

                  <h6 class="text-uppercase text-muted text-sm mt-3 mb-2">Commentaires</h6>
                  <ul class="list-unstyled mb-2">
                    <li *ngFor="let comment of commentsFor(task.id)" class="comment-item">
                      <div class="d-flex justify-content-between align-items-start">
                        <strong class="text-sm">{{ comment.authorName }}</strong>
                        <button
                          *ngIf="canDelete(comment.authorId)"
                          type="button"
                          class="btn btn-sm btn-link text-danger p-0"
                          (click)="deleteComment(task, comment)"
                        >
                          Supprimer
                        </button>
                      </div>
                      <p class="mb-1 text-sm">{{ comment.content }}</p>
                      <small class="text-muted">{{ comment.createdAt | date: 'dd/MM/yyyy HH:mm' }}</small>
                    </li>
                    <li *ngIf="commentsFor(task.id).length === 0" class="text-muted text-sm">Aucun commentaire.</li>
                  </ul>
                  <form [formGroup]="commentForm" (ngSubmit)="addComment(task)" class="d-flex align-items-start mb-3">
                    <textarea
                      class="form-control form-control-sm mr-2"
                      rows="1"
                      formControlName="content"
                      placeholder="Écrire un commentaire…"
                    ></textarea>
                    <button type="submit" class="btn btn-sm btn-primary" [disabled]="commentForm.invalid">Publier</button>
                  </form>

                  <h6 class="text-uppercase text-muted text-sm mt-3 mb-2">
                    Temps passé
                    <span class="text-dark">({{ totalHours(task.id) }} h)</span>
                  </h6>
                  <ul class="list-unstyled mb-2">
                    <li *ngFor="let entry of timeEntriesFor(task.id)" class="comment-item">
                      <div class="d-flex justify-content-between align-items-start">
                        <span class="text-sm">
                          <strong>{{ entry.hours }} h</strong> — {{ entry.description || 'Sans description' }}
                        </span>
                        <button
                          *ngIf="canDelete(entry.userId)"
                          type="button"
                          class="btn btn-sm btn-link text-danger p-0"
                          (click)="deleteTimeEntry(task, entry)"
                        >
                          Supprimer
                        </button>
                      </div>
                      <small class="text-muted">{{ entry.userName }} · {{ entry.entryDate | date: 'dd/MM/yyyy' }}</small>
                    </li>
                    <li *ngIf="timeEntriesFor(task.id).length === 0" class="text-muted text-sm">Aucun temps pointé.</li>
                  </ul>
                  <form [formGroup]="timeForm" (ngSubmit)="addTimeEntry(task)" class="row">
                    <div class="col-4">
                      <input class="form-control form-control-sm" type="number" min="0.25" step="0.25" formControlName="hours" />
                    </div>
                    <div class="col-4">
                      <input class="form-control form-control-sm" type="date" formControlName="entryDate" />
                    </div>
                    <div class="col-4">
                      <button type="submit" class="btn btn-sm btn-outline-primary btn-block" [disabled]="timeForm.invalid">
                        Pointer
                      </button>
                    </div>
                    <div class="col-12 mt-2">
                      <input class="form-control form-control-sm" formControlName="description" placeholder="Description (optionnel)" />
                    </div>
                  </form>
                </div>
              </article>
              <p *ngIf="tasksFor(column.status).length === 0" class="text-muted text-sm text-center mb-0 py-3">
                Aucune tâche dans cette colonne.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    '.board-column { min-height: 22rem; }',
    '.board-body { display: flex; min-height: 18rem; flex-direction: column; gap: 0.75rem; background: #f8f7f5; }',
    '.task-card { padding: 0.85rem; border: 1px solid #e7e5e4; border-left: 4px solid #b84c13; border-radius: 0.55rem; background: #fff; cursor: grab; }',
    '.task-card:active { cursor: grabbing; }',
    '.task-card--overdue { border-left-color: #e11d48; background: #fff5f6; }',
    '.task-card--done { border-left-color: #2dce89; opacity: 0.85; }',
    '.task-title { overflow-wrap: anywhere; }',
    '.task-details { padding-top: 0.75rem; border-top: 1px dashed #d6d3d1; }',
    '.comment-item { padding: 0.5rem 0; border-bottom: 1px solid #f0eeec; }',
    '.comment-item:last-child { border-bottom: 0; }',
  ],
})
export class TasksComponent implements OnInit {
  readonly columns: BoardColumn[] = [
    { status: 'TODO', label: 'À faire', icon: 'ni-bullet-list-67', accent: 'text-warning' },
    { status: 'IN_PROGRESS', label: 'En cours', icon: 'ni-settings-gear-65', accent: 'text-info' },
    { status: 'DONE', label: 'Terminées', icon: 'ni-check-bold', accent: 'text-success' },
  ];

  readonly isAdmin: boolean;

  tasks: TaskModel[] = [];
  projects: ProjectModel[] = [];
  users: UserModel[] = [];
  stats: TaskStats = {
    total: 0,
    todo: 0,
    inProgress: 0,
    done: 0,
    overdue: 0,
    urgent: 0,
    totalHours: 0,
  };

  editingId: number | null = null;
  saving = false;
  expandedTaskId: number | null = null;
  currentUserId: number | null = null;

  filterProjectId: number | null = null;
  filterAssigneeId: number | null = null;
  filterPriority: 'ALL' | TaskPriority = 'ALL';
  search = '';

  readonly taskForm: FormGroup<{
    title: FormControl<string | null>;
    projectId: FormControl<number | null>;
    assignedUserId: FormControl<number | null>;
    status: FormControl<TaskStatus | null>;
    priority: FormControl<TaskPriority | null>;
    startDate: FormControl<string | null>;
    dueDate: FormControl<string | null>;
    description: FormControl<string | null>;
  }>;

  readonly commentForm: FormGroup<{ content: FormControl<string | null> }>;
  readonly timeForm: FormGroup<{
    hours: FormControl<number | null>;
    entryDate: FormControl<string | null>;
    description: FormControl<string | null>;
  }>;

  private readonly commentsCache = new Map<number, TaskCommentModel[]>();
  private readonly timeCache = new Map<number, TimeEntryModel[]>();
  private draggedTask: TaskModel | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly taskService: TaskService,
    private readonly projectService: ProjectService,
    private readonly userService: UserService,
    private readonly authService: AuthService,
    private readonly errors: ErrorNotificationService,
    private readonly changeDetector: ChangeDetectorRef,
  ) {
    this.isAdmin = authService.isAdmin();
    this.taskForm = this.fb.group({
      title: this.fb.control<string | null>('', Validators.required),
      projectId: this.fb.control<number | null>(null, Validators.required),
      assignedUserId: this.fb.control<number | null>(null),
      status: this.fb.control<TaskStatus | null>('TODO'),
      priority: this.fb.control<TaskPriority | null>('MEDIUM'),
      startDate: this.fb.control<string | null>(null),
      dueDate: this.fb.control<string | null>(null),
      description: this.fb.control<string | null>(''),
    });
    this.commentForm = this.fb.group({
      content: this.fb.control<string | null>('', Validators.required),
    });
    this.timeForm = this.fb.group({
      hours: this.fb.control<number | null>(1, [Validators.required, Validators.min(0.25)]),
      entryDate: this.fb.control<string | null>(this.today(), Validators.required),
      description: this.fb.control<string | null>(''),
    });

    this.authService.profile$
      .pipe(takeUntilDestroyed())
      .subscribe((profile) => {
        this.currentUserId = profile?.id ?? null;
        this.changeDetector.markForCheck();
      });
  }

  ngOnInit(): void {
    this.loadProjects();
    this.loadUsers();
    this.loadTasks();
    this.loadStats();
  }

  get visibleTasks(): TaskModel[] {
    const search = this.search.trim().toLowerCase();
    return this.tasks.filter((task) => {
      if (this.filterProjectId && task.projectId !== this.filterProjectId) {
        return false;
      }
      if (this.filterAssigneeId && task.assignedUserId !== this.filterAssigneeId) {
        return false;
      }
      if (this.filterPriority !== 'ALL' && task.priority !== this.filterPriority) {
        return false;
      }
      if (!search) {
        return true;
      }
      return (
        task.title.toLowerCase().includes(search) ||
        (task.description ?? '').toLowerCase().includes(search) ||
        task.projectLibelle.toLowerCase().includes(search)
      );
    });
  }

  tasksFor(status: TaskStatus): TaskModel[] {
    return this.visibleTasks.filter((task) => task.status === status);
  }

  loadProjects(): void {
    this.projectService.getAll().subscribe((projects) => {
      this.projects = projects;
      this.changeDetector.markForCheck();
    });
  }

  loadUsers(): void {
    this.userService.getDirectory().subscribe({
      next: (users) => {
        this.users = users;
        this.changeDetector.markForCheck();
      },
      error: () => undefined,
    });
  }

  loadTasks(): void {
    this.taskService.getAll().subscribe((tasks) => {
      this.tasks = tasks;
      this.errors.dismiss();
      this.changeDetector.markForCheck();
    });
  }

  loadStats(): void {
    this.taskService.getStats().subscribe((stats) => {
      this.stats = stats;
      this.changeDetector.markForCheck();
    });
  }

  submitTask(): void {
    if (this.taskForm.invalid || this.saving) {
      this.taskForm.markAllAsTouched();
      return;
    }

    const raw = this.taskForm.getRawValue();
    if (!raw.projectId) {
      return;
    }

    const payload: TaskRequest = {
      title: raw.title?.trim() ?? '',
      description: raw.description?.trim() || null,
      status: raw.status ?? 'TODO',
      priority: raw.priority ?? 'MEDIUM',
      startDate: raw.startDate,
      dueDate: raw.dueDate,
      projectId: raw.projectId,
      assignedUserId: raw.assignedUserId,
    };

    this.saving = true;
    const request = this.editingId ? this.taskService.update(this.editingId, payload) : this.taskService.create(payload);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.loadTasks();
        this.loadStats();
        this.resetTaskForm();
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.saving = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  editTask(task: TaskModel): void {
    this.editingId = task.id;
    this.taskForm.patchValue({
      title: task.title,
      projectId: task.projectId,
      assignedUserId: task.assignedUserId,
      status: task.status,
      priority: task.priority,
      startDate: task.startDate,
      dueDate: task.dueDate,
      description: task.description,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.changeDetector.markForCheck();
  }

  deleteTask(task: TaskModel): void {
    if (!window.confirm(`Supprimer la tâche « ${task.title} » ?`)) {
      return;
    }
    this.taskService.delete(task.id).subscribe(() => {
      this.commentsCache.delete(task.id);
      this.timeCache.delete(task.id);
      if (this.expandedTaskId === task.id) {
        this.expandedTaskId = null;
      }
      this.loadTasks();
      this.loadStats();
      this.changeDetector.markForCheck();
    });
  }

  moveTask(task: TaskModel, status: TaskStatus): void {
    if (task.status === status) {
      return;
    }
    this.taskService.updateStatus(task.id, status).subscribe(() => {
      this.loadTasks();
      this.loadStats();
      this.changeDetector.markForCheck();
    });
  }

  startDrag(task: TaskModel): void {
    this.draggedTask = task;
  }

  endDrag(): void {
    this.draggedTask = null;
  }

  allowDrop(event: DragEvent): void {
    event.preventDefault();
  }

  dropOn(status: TaskStatus): void {
    if (!this.draggedTask) {
      return;
    }
    const task = this.draggedTask;
    this.draggedTask = null;
    this.moveTask(task, status);
  }

  toggleDetails(task: TaskModel): void {
    if (this.expandedTaskId === task.id) {
      this.expandedTaskId = null;
      this.changeDetector.markForCheck();
      return;
    }
    this.expandedTaskId = task.id;
    this.commentForm.reset({ content: '' });
    this.timeForm.reset({ hours: 1, entryDate: this.today(), description: '' });
    this.loadComments(task.id);
    this.loadTimeEntries(task.id);
    this.changeDetector.markForCheck();
  }

  collapseDetails(): void {
    this.expandedTaskId = null;
    this.changeDetector.markForCheck();
  }

  commentsFor(taskId: number): TaskCommentModel[] {
    return this.commentsCache.get(taskId) ?? [];
  }

  timeEntriesFor(taskId: number): TimeEntryModel[] {
    return this.timeCache.get(taskId) ?? [];
  }

  totalHours(taskId: number): number {
    return Math.round(this.timeEntriesFor(taskId).reduce((sum, entry) => sum + Number(entry.hours ?? 0), 0) * 100) / 100;
  }

  addComment(task: TaskModel): void {
    const content = this.commentForm.controls.content.value?.trim();
    if (!content) {
      return;
    }
    this.taskService.addComment(task.id, content).subscribe(() => {
      this.commentForm.reset({ content: '' });
      this.loadComments(task.id);
    });
  }

  deleteComment(task: TaskModel, comment: TaskCommentModel): void {
    if (!window.confirm('Supprimer ce commentaire ?')) {
      return;
    }
    this.taskService.deleteComment(task.id, comment.id).subscribe(() => this.loadComments(task.id));
  }

  addTimeEntry(task: TaskModel): void {
    if (this.timeForm.invalid) {
      return;
    }
    const raw = this.timeForm.getRawValue();
    this.taskService
      .addTimeEntry(task.id, {
        hours: Number(raw.hours ?? 0),
        description: raw.description?.trim() || null,
        entryDate: raw.entryDate ?? this.today(),
      })
      .subscribe(() => {
        this.timeForm.reset({ hours: 1, entryDate: this.today(), description: '' });
        this.loadTimeEntries(task.id);
        this.loadStats();
      });
  }

  deleteTimeEntry(task: TaskModel, entry: TimeEntryModel): void {
    if (!window.confirm('Supprimer cette entrée de temps ?')) {
      return;
    }
    this.taskService.deleteTimeEntry(task.id, entry.id).subscribe(() => {
      this.loadTimeEntries(task.id);
      this.loadStats();
    });
  }

  canDelete(ownerId: number): boolean {
    return this.isAdmin || (this.currentUserId !== null && this.currentUserId === ownerId);
  }

  priorityLabel(priority: TaskPriority): string {
    switch (priority) {
      case 'LOW':
        return 'Basse';
      case 'HIGH':
        return 'Haute';
      case 'URGENT':
        return 'Urgente';
      default:
        return 'Moyenne';
    }
  }

  priorityClass(priority: TaskPriority): string {
    switch (priority) {
      case 'URGENT':
        return 'badge-danger';
      case 'HIGH':
        return 'badge-warning';
      case 'LOW':
        return 'badge-light';
      default:
        return 'badge-info';
    }
  }

  resetTaskForm(): void {
    this.editingId = null;
    this.taskForm.reset({
      title: '',
      projectId: null,
      assignedUserId: null,
      status: 'TODO',
      priority: 'MEDIUM',
      startDate: null,
      dueDate: null,
      description: '',
    });
    this.changeDetector.markForCheck();
  }

  private loadComments(taskId: number): void {
    this.taskService.getComments(taskId).subscribe((comments) => {
      this.commentsCache.set(taskId, comments);
      this.changeDetector.markForCheck();
    });
  }

  private loadTimeEntries(taskId: number): void {
    this.taskService.getTimeEntries(taskId).subscribe((entries) => {
      this.timeCache.set(taskId, entries);
      this.changeDetector.markForCheck();
    });
  }

  private today(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  }
}
