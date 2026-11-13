import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Client, ProjectModel } from '../../models';
import { AuthService } from '../../services/auth.service';
import { ClientService } from '../../services/client.service';
import { ErrorNotificationService } from '../../services/error-notification.service';
import { ProjectService } from '../../services/project.service';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="container-fluid py-4">
      <div class="row">
        <div class="col-xl-4 col-lg-5 mb-4">
          <div class="card shadow">
            <div class="card-header border-0">
              <h2 class="h3 mb-0">{{ editingId ? 'Modifier le projet' : 'Nouveau projet' }}</h2>
            </div>
            <div class="card-body">
              <form [formGroup]="form" (ngSubmit)="submit()">
                <div class="form-group">
                  <label class="form-control-label" for="project-libelle">Libellé</label>
                  <input id="project-libelle" class="form-control" formControlName="libelle" />
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="project-client">Client</label>
                  <select id="project-client" class="form-control" formControlName="clientId">
                    <option [ngValue]="null" disabled>Choisir un client</option>
                    <option *ngFor="let client of clients" [ngValue]="client.id">{{ client.nom }}</option>
                  </select>
                </div>
                <div class="row">
                  <div class="col-sm-6">
                    <div class="form-group">
                      <label class="form-control-label" for="project-budget">Budget</label>
                      <input id="project-budget" class="form-control" type="number" formControlName="budget" min="0" />
                    </div>
                  </div>
                  <div class="col-sm-6">
                    <div class="form-group">
                      <label class="form-control-label" for="project-duree">Durée (jours)</label>
                      <input id="project-duree" class="form-control" type="number" formControlName="duree" min="1" />
                    </div>
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="project-description">Description</label>
                  <textarea id="project-description" class="form-control" rows="2" formControlName="description"></textarea>
                </div>

                <div class="form-group">
                  <label class="form-control-label" for="project-image">Image du projet</label>
                  <input
                    #imageInput
                    id="project-image"
                    class="form-control-file"
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    (change)="onImageSelected($event)"
                  />
                  <small class="text-muted d-block mt-1">JPG, PNG, GIF ou WebP — 10 Mo max.</small>
                  <div class="mt-2" *ngIf="imagePreviewUrl || existingImageUrl">
                    <img
                      class="project-image-preview"
                      [src]="imagePreviewUrl ?? existingImageUrl"
                      alt="Aperçu de l'image du projet"
                    />
                    <button type="button" class="btn btn-sm btn-link text-danger" (click)="removeImage()">
                      Supprimer l'image
                    </button>
                  </div>
                </div>

                <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving">
                  {{ saving ? 'Enregistrement…' : editingId ? 'Enregistrer' : 'Ajouter le projet' }}
                </button>
                <button type="button" class="btn btn-link text-muted" *ngIf="editingId" (click)="resetForm()">Annuler</button>
              </form>
            </div>
          </div>
        </div>

        <div class="col-xl-8 col-lg-7 mb-4">
          <div class="card shadow">
            <div class="card-header border-0">
              <h2 class="h3 mb-0">Liste des projets</h2>
            </div>
            <div class="table-responsive">
              <table class="table align-items-center table-flush">
                <thead class="thead-light">
                  <tr>
                    <th scope="col">Image</th>
                    <th scope="col">Libellé</th>
                    <th scope="col">Client</th>
                    <th scope="col">Budget</th>
                    <th scope="col">Durée</th>
                    <th scope="col" *ngIf="isAdmin">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let project of projects">
                    <td>
                      <img
                        *ngIf="imageFor(project); else noProjectImage"
                        class="project-thumb"
                        [src]="imageFor(project)"
                        [alt]="project.libelle"
                      />
                      <ng-template #noProjectImage>
                        <span class="project-thumb project-thumb--empty"><i class="ni ni-image"></i></span>
                      </ng-template>
                    </td>
                    <th scope="row">
                      <span class="d-block">{{ project.libelle }}</span>
                      <small class="text-muted text-truncate d-block project-description" *ngIf="project.description">
                        {{ project.description }}
                      </small>
                    </th>
                    <td>{{ project.client?.nom ?? '—' }}</td>
                    <td>{{ project.budget | currency:'EUR' }}</td>
                    <td>{{ project.duree }} jours</td>
                    <td *ngIf="isAdmin">
                      <div class="d-flex">
                        <button type="button" class="btn btn-sm btn-outline-primary mr-2" (click)="edit(project)">Modifier</button>
                        <button type="button" class="btn btn-sm btn-outline-danger" (click)="delete(project.id!)">Supprimer</button>
                      </div>
                    </td>
                  </tr>
                  <tr *ngIf="projects.length === 0">
                    <td [attr.colspan]="isAdmin ? 6 : 5" class="text-center text-muted py-4">Aucun projet trouvé.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    '.project-thumb { width: 54px; height: 54px; object-fit: cover; border-radius: 0.45rem; border: 1px solid #e7e5e4; background: #fff; }',
    '.project-thumb--empty { display: inline-flex; align-items: center; justify-content: center; color: #a8a29e; font-size: 1.15rem; background: #f5f5f4; }',
    '.project-image-preview { display: block; width: 140px; height: 100px; object-fit: cover; border-radius: 0.5rem; border: 1px solid #e7e5e4; margin-bottom: 0.35rem; }',
    '.project-description { max-width: 18rem; }',
  ],
})
export class ProjectsComponent implements OnInit, OnDestroy {
  @ViewChild('imageInput') private imageInput?: ElementRef<HTMLInputElement>;

  readonly maxImageSize = 10 * 1024 * 1024;
  readonly isAdmin: boolean;

  projects: ProjectModel[] = [];
  clients: Client[] = [];
  editingId: number | null = null;
  saving = false;

  selectedImage: File | null = null;
  imagePreviewUrl: string | null = null;
  existingImageUrl: string | null = null;

  form: ReturnType<FormBuilder['group']>;

  private readonly projectImages = new Map<number, string>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly projectService: ProjectService,
    private readonly clientService: ClientService,
    authService: AuthService,
    private readonly errors: ErrorNotificationService,
    private readonly changeDetector: ChangeDetectorRef,
  ) {
    this.isAdmin = authService.isAdmin();
    this.form = this.fb.group({
      libelle: ['', Validators.required],
      budget: [0, [Validators.required, Validators.min(0)]],
      duree: [1, [Validators.required, Validators.min(1)]],
      description: [''],
      clientId: [null as number | null, Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadClients();
    this.loadProjects();
  }

  ngOnDestroy(): void {
    this.clearProjectImages();
    this.revokeImagePreview();
    if (this.existingImageUrl) {
      URL.revokeObjectURL(this.existingImageUrl);
      this.existingImageUrl = null;
    }
  }

  loadClients(): void {
    this.clientService.getAll().subscribe((clients) => {
      this.clients = clients;
      this.changeDetector.markForCheck();
    });
  }

  loadProjects(): void {
    this.projectService.getAll().subscribe((projects) => {
      this.projects = projects;
      this.errors.dismiss();
      this.refreshProjectImages();
      this.changeDetector.markForCheck();
    });
  }

  imageFor(project: ProjectModel): string | undefined {
    return project.id ? this.projectImages.get(project.id) : undefined;
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);
    if (!file) {
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.errors.show('Seules les images sont autorisées pour la photo du projet.');
      input.value = '';
      return;
    }
    if (file.size > this.maxImageSize) {
      this.errors.show("L'image dépasse la taille maximale de 10 Mo.");
      input.value = '';
      return;
    }

    this.revokeImagePreview();
    this.selectedImage = file;
    this.imagePreviewUrl = URL.createObjectURL(file);
    this.changeDetector.markForCheck();
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue() as {
      libelle: string | null;
      budget: number | null;
      duree: number | null;
      description: string | null;
      clientId: number | null;
    };

    const clientId = Number(raw.clientId ?? 0);
    if (!clientId) {
      return;
    }

    const payload: ProjectModel = {
      libelle: raw.libelle ?? '',
      budget: Number(raw.budget ?? 0),
      duree: Number(raw.duree ?? 1),
      description: raw.description ?? '',
      clientId,
    };

    this.saving = true;
    const request = this.editingId
      ? this.projectService.update(this.editingId, payload, clientId)
      : this.projectService.create(payload, clientId);

    request.subscribe({
      next: (project) => {
        if (this.selectedImage && project.id) {
          this.projectService.uploadImage(project.id, this.selectedImage).subscribe({
            next: () => this.finishSave(),
            error: () => this.finishSave(),
          });
          return;
        }
        this.finishSave();
      },
      error: () => {
        this.saving = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  edit(project: ProjectModel): void {
    this.editingId = project.id ?? null;
    this.selectedImage = null;
    this.revokeImagePreview();
    if (this.imageInput) {
      this.imageInput.nativeElement.value = '';
    }
    this.form.patchValue({
      libelle: project.libelle,
      budget: project.budget,
      duree: project.duree,
      description: project.description,
      clientId: project.clientId ?? project.client?.id ?? null,
    });
    this.loadExistingImage(project);
    this.changeDetector.markForCheck();
  }

  removeImage(): void {
    if (this.editingId && this.existingImageUrl) {
      const id = this.editingId;
      this.projectService.deleteImage(id).subscribe(() => {
        this.clearExistingImage();
        this.loadProjects();
        this.changeDetector.markForCheck();
      });
      return;
    }
    this.selectedImage = null;
    this.revokeImagePreview();
    if (this.imageInput) {
      this.imageInput.nativeElement.value = '';
    }
    this.changeDetector.markForCheck();
  }

  delete(id: number): void {
    if (!window.confirm('Supprimer ce projet ?')) {
      return;
    }
    this.projectService.delete(id).subscribe(() => {
      this.loadProjects();
      if (this.editingId === id) {
        this.resetForm();
      }
      this.changeDetector.markForCheck();
    });
  }

  resetForm(): void {
    this.editingId = null;
    this.selectedImage = null;
    this.revokeImagePreview();
    this.clearExistingImage();
    if (this.imageInput) {
      this.imageInput.nativeElement.value = '';
    }
    this.form.reset({
      libelle: '',
      budget: 0,
      duree: 1,
      description: '',
      clientId: null,
    });
    this.changeDetector.markForCheck();
  }

  private finishSave(): void {
    this.saving = false;
    this.loadProjects();
    this.resetForm();
    this.changeDetector.markForCheck();
  }

  private loadExistingImage(project: ProjectModel): void {
    this.clearExistingImage();
    if (!project.imageUrl || !project.id) {
      return;
    }
    this.projectService.getImage(project.id).subscribe({
      next: (blob) => {
        this.existingImageUrl = URL.createObjectURL(blob);
        this.changeDetector.markForCheck();
      },
      error: () => undefined,
    });
  }

  private refreshProjectImages(): void {
    this.clearProjectImages();
    for (const project of this.projects) {
      if (!project.id || !project.imageUrl) {
        continue;
      }
      const id = project.id;
      this.projectService.getImage(id).subscribe({
        next: (blob) => {
          this.projectImages.set(id, URL.createObjectURL(blob));
          this.changeDetector.markForCheck();
        },
        error: () => undefined,
      });
    }
  }

  private clearProjectImages(): void {
    for (const url of this.projectImages.values()) {
      URL.revokeObjectURL(url);
    }
    this.projectImages.clear();
  }

  private revokeImagePreview(): void {
    if (this.imagePreviewUrl) {
      URL.revokeObjectURL(this.imagePreviewUrl);
      this.imagePreviewUrl = null;
    }
  }

  private clearExistingImage(): void {
    if (this.existingImageUrl) {
      URL.revokeObjectURL(this.existingImageUrl);
      this.existingImageUrl = null;
    }
  }
}
