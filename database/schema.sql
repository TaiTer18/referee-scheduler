-- Referee Scheduler Database Schema

-- Users table (Referees and Admins)
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(20) NOT NULL, -- 'REFEREE' or 'ADMIN'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Games table
CREATE TABLE games (
    id SERIAL PRIMARY KEY,
    game_date DATE NOT NULL,
    game_time TIME NOT NULL,
    location VARCHAR(255) NOT NULL,
    home_team VARCHAR(255) NOT NULL,
    away_team VARCHAR(255) NOT NULL,
    age_group VARCHAR(50),
    status VARCHAR(20) DEFAULT 'OPEN', -- 'OPEN', 'ASSIGNED', 'COMPLETED', 'CANCELLED'
    assigned_referee_id INTEGER,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (assigned_referee_id) REFERENCES users(id)
);

-- Referee Availability table (Many-to-Many)
CREATE TABLE referee_availability (
    id SERIAL PRIMARY KEY,
    referee_id INTEGER NOT NULL,
    game_id INTEGER NOT NULL,
    is_available BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(referee_id, game_id),
    FOREIGN KEY (referee_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

-- Game Assignments table (History)
CREATE TABLE game_assignments (
    id SERIAL PRIMARY KEY,
    game_id INTEGER NOT NULL,
    referee_id INTEGER NOT NULL,
    assigned_by_admin_id INTEGER NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ASSIGNED', -- 'ASSIGNED', 'CONFIRMED', 'CANCELLED'
    notes TEXT,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY (referee_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by_admin_id) REFERENCES users(id)
);

-- Create indexes for faster queries
CREATE INDEX idx_games_assigned_referee ON games(assigned_referee_id);
CREATE INDEX idx_referee_availability_referee ON referee_availability(referee_id);
CREATE INDEX idx_referee_availability_game ON referee_availability(game_id);
CREATE INDEX idx_game_assignments_game ON game_assignments(game_id);
CREATE INDEX idx_game_assignments_referee ON game_assignments(referee_id);