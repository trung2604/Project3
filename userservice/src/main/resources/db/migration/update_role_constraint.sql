-- Update check constraint for users.role column to include all roles
-- Run this script in PostgreSQL database

-- Step 1: Drop the existing constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Step 2: Add new constraint with all roles
ALTER TABLE users ADD CONSTRAINT users_role_check 
CHECK (role IN ('CUSTOMER', 'STAFF', 'WAREHOUSE_STAFF', 'RESTAURANT_MANAGER', 'ADMIN'));

-- Step 3: Verify the constraint (optional)
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'users'::regclass
AND conname = 'users_role_check';

