import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredPermissions = route.data['permissions'] as string[];
  const requiredRoles = route.data['roles'] as string[];

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  if (requiredPermissions && requiredPermissions.length > 0) {
    const hasPerm = authService.hasAnyPermission(requiredPermissions);
    if (!hasPerm) {
      return router.createUrlTree(['/unauthorized']);
    }
  }

  if (requiredRoles && requiredRoles.length > 0) {
    const hasRole = requiredRoles.includes(authService.userRole());
    if (!hasRole) {
      return router.createUrlTree(['/unauthorized']);
    }
  }

  return true;
};
