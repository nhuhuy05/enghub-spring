INSERT INTO user_roles (user_id, role_id)
SELECT ur.user_id, admin_role.id
FROM user_roles ur
JOIN roles teacher_role ON teacher_role.id = ur.role_id
JOIN roles admin_role ON admin_role.name = 'ADMIN'
WHERE teacher_role.name = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles existing_admin_role
      WHERE existing_admin_role.user_id = ur.user_id
        AND existing_admin_role.role_id = admin_role.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT admin_role.id, rp.permission_id
FROM role_permissions rp
JOIN roles teacher_role ON teacher_role.id = rp.role_id
JOIN roles admin_role ON admin_role.name = 'ADMIN'
WHERE teacher_role.name = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing_admin_permission
      WHERE existing_admin_permission.role_id = admin_role.id
        AND existing_admin_permission.permission_id = rp.permission_id
  );

DELETE FROM user_roles
WHERE role_id IN (
    SELECT id
    FROM roles
    WHERE name = 'TEACHER'
);

DELETE FROM role_permissions
WHERE role_id IN (
    SELECT id
    FROM roles
    WHERE name = 'TEACHER'
);

DELETE FROM roles
WHERE name = 'TEACHER';
