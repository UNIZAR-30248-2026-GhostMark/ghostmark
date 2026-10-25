export type AccountField = "alias" | "email" | "contrasena";
export type AccountFieldErrors = Partial<Record<AccountField, string>>;

const emailPattern = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const aliasPattern = /^[A-Za-z0-9_]{3,20}$/;

export function validateRegistration(alias: string, email: string, password: string): AccountFieldErrors {
  const errors: AccountFieldErrors = {};
  if (!aliasPattern.test(alias.trim())) errors.alias = "De 3 a 20: letras sin tildes, números y guion bajo";
  if (!emailPattern.test(email.trim()) || email.trim().length > 254) errors.email = "Debe ser un email válido";
  if (password.length < 8 || password.length > 72) errors.contrasena = "Debe tener entre 8 y 72 caracteres";
  return errors;
}

export function validateSignIn(email: string, password: string): AccountFieldErrors {
  const errors: AccountFieldErrors = {};
  if (!emailPattern.test(email.trim()) || email.trim().length > 254) errors.email = "Debe ser un email válido";
  if (!password) errors.contrasena = "Introduce tu contraseña";
  return errors;
}

export function hasAccountFieldErrors(errors: AccountFieldErrors) {
  return Object.keys(errors).length > 0;
}
