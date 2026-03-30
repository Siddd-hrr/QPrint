const checks = [
  { label: 'At least 8 characters', test: (p) => p.length >= 8 },
  { label: 'One uppercase letter', test: (p) => /[A-Z]/.test(p) },
  { label: 'One lowercase letter', test: (p) => /[a-z]/.test(p) },
  { label: 'One digit', test: (p) => /\d/.test(p) },
  { label: 'One special character (@$!%*?&)', test: (p) => /[@$!%*?&]/.test(p) },
];

export function getPasswordChecks(password) {
  return checks.map((c) => ({ ...c, ok: c.test(password || '') }));
}

export function getPasswordScore(checkResults) {
  return checkResults.filter((c) => c.ok).length;
}

export function passwordRegex() {
  return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
}
