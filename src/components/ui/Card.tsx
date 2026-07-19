import type { ReactNode } from 'react';
import { cn } from '@/utils/cn';

interface CardProps {
  children: ReactNode;
  className?: string;
}

interface CardHeaderProps {
  children: ReactNode;
  className?: string;
}

interface CardTitleProps {
  children: ReactNode;
  className?: string;
  as?: 'h1' | 'h2' | 'h3' | 'h4';
}

export function Card({ children, className }: CardProps) {
  return (
    <div className={cn('rounded-xl border border-surface-200 bg-white', className)}>
      {children}
    </div>
  );
}

export function CardHeader({ children, className }: CardHeaderProps) {
  return (
    <div className={cn('flex items-center justify-between border-b border-surface-100 px-6 py-4', className)}>
      {children}
    </div>
  );
}

export function CardTitle({ children, className, as: Tag = 'h3' }: CardTitleProps) {
  return (
    <Tag className={cn('text-base font-semibold text-surface-900', className)}>
      {children}
    </Tag>
  );
}

export function CardContent({ children, className }: CardProps) {
  return <div className={cn('px-6 py-4', className)}>{children}</div>;
}

export function CardFooter({ children, className }: CardProps) {
  return (
    <div className={cn('flex items-center border-t border-surface-100 px-6 py-4', className)}>
      {children}
    </div>
  );
}
