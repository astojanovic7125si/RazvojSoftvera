-- Šema baze koja odgovara entitetima.
-- U razvoju tabele pravi Hibernate (ddl-auto=create-drop), pa ova skripta nije potrebna.
-- Koristi se za produkciju (ddl-auto=validate), a kasnije može da postane Flyway migracija V1.

CREATE DATABASE IF NOT EXISTS raf_lms
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE raf_lms;

-- 1. Korisnički nalozi
CREATE TABLE app_users (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           username VARCHAR(100) NOT NULL,
                           password_hash VARCHAR(255) NOT NULL,
                           role VARCHAR(20) NOT NULL,
                           first_name VARCHAR(100) NOT NULL,
                           last_name VARCHAR(100) NOT NULL,

                           PRIMARY KEY (id),
                           CONSTRAINT uk_app_users_username UNIQUE (username)
) ENGINE = InnoDB;


-- 2. Studenti
CREATE TABLE students (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          user_id BIGINT NOT NULL,
                          index_number VARCHAR(30) NOT NULL,

                          PRIMARY KEY (id),

                          CONSTRAINT uk_students_user UNIQUE (user_id),
                          CONSTRAINT uk_students_index UNIQUE (index_number),

                          CONSTRAINT fk_students_user
                              FOREIGN KEY (user_id)
                                  REFERENCES app_users(id)
) ENGINE = InnoDB;


-- 3. Predmeti
CREATE TABLE courses (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         code VARCHAR(30) NOT NULL,
                         name VARCHAR(150) NOT NULL,

                         PRIMARY KEY (id),
                         CONSTRAINT uk_courses_code UNIQUE (code)
) ENGINE = InnoDB;


-- 3a. Nastavnici koji drže predmet (veza više-na-više)
CREATE TABLE course_teachers (
                                 course_id BIGINT NOT NULL,
                                 user_id BIGINT NOT NULL,

                                 PRIMARY KEY (course_id, user_id),

                                 CONSTRAINT fk_course_teachers_course
                                     FOREIGN KEY (course_id)
                                         REFERENCES courses(id),

                                 CONSTRAINT fk_course_teachers_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES app_users(id)
) ENGINE = InnoDB;


-- 4. Ispitni rokovi
CREATE TABLE exam_periods (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              name VARCHAR(100) NOT NULL,
                              start_date DATE NOT NULL,
                              end_date DATE NOT NULL,

                              PRIMARY KEY (id),

                              CONSTRAINT chk_exam_period_dates
                                  CHECK (end_date >= start_date)
) ENGINE = InnoDB;


-- 5. Ispitni termini
CREATE TABLE exam_sessions (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               course_id BIGINT NOT NULL,
                               exam_period_id BIGINT NOT NULL,
                               start_time DATETIME(6) NOT NULL,
                               end_time DATETIME(6) NOT NULL,
                               max_points DECIMAL(7, 2) NOT NULL,

                               PRIMARY KEY (id),

                               CONSTRAINT fk_exam_sessions_course
                                   FOREIGN KEY (course_id)
                                       REFERENCES courses(id),

                               CONSTRAINT fk_exam_sessions_period
                                   FOREIGN KEY (exam_period_id)
                                       REFERENCES exam_periods(id),

                               CONSTRAINT chk_exam_session_times
                                   CHECK (end_time > start_time),

                               CONSTRAINT chk_exam_session_max_points
                                   CHECK (max_points > 0)
) ENGINE = InnoDB;


-- 6. Ispitne grupe
CREATE TABLE exam_groups (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             exam_session_id BIGINT NOT NULL,
                             name VARCHAR(50) NOT NULL,

                             PRIMARY KEY (id),

                             CONSTRAINT uk_exam_group_session_name
                                 UNIQUE (exam_session_id, name),

                             CONSTRAINT fk_exam_groups_session
                                 FOREIGN KEY (exam_session_id)
                                     REFERENCES exam_sessions(id)
) ENGINE = InnoDB;


-- 7. Prijave ispita
CREATE TABLE exam_registrations (
                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                    student_id BIGINT NOT NULL,
                                    exam_session_id BIGINT NOT NULL,
                                    exam_group_id BIGINT,
                                    classroom VARCHAR(50),

                                    PRIMARY KEY (id),

                                    CONSTRAINT uk_exam_registration_student_session
                                        UNIQUE (student_id, exam_session_id),

                                    CONSTRAINT fk_registrations_student
                                        FOREIGN KEY (student_id)
                                            REFERENCES students(id),

                                    CONSTRAINT fk_registrations_session
                                        FOREIGN KEY (exam_session_id)
                                            REFERENCES exam_sessions(id),

                                    CONSTRAINT fk_registrations_group
                                        FOREIGN KEY (exam_group_id)
                                            REFERENCES exam_groups(id)
) ENGINE = InnoDB;


-- 8. Pokušaji polaganja
CREATE TABLE exam_attempts (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               exam_registration_id BIGINT NOT NULL,
                               started_at DATETIME(6) NOT NULL,
                               finished_at DATETIME(6),

                               PRIMARY KEY (id),

                               CONSTRAINT uk_exam_attempt_registration
                                   UNIQUE (exam_registration_id),

                               CONSTRAINT fk_attempts_registration
                                   FOREIGN KEY (exam_registration_id)
                                       REFERENCES exam_registrations(id),

                               CONSTRAINT chk_attempt_times
                                   CHECK (
                                       finished_at IS NULL
                                           OR finished_at >= started_at
                                       )
) ENGINE = InnoDB;


-- 9. Ispitni zadaci
CREATE TABLE assignments (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             exam_session_id BIGINT NOT NULL,
                             exam_group_id BIGINT,
                             title VARCHAR(150) NOT NULL,
                             description LONGTEXT,
                             original_file_name VARCHAR(255),
                             storage_key VARCHAR(500),

                             PRIMARY KEY (id),

                             CONSTRAINT uk_assignments_storage_key
                                 UNIQUE (storage_key),

                             CONSTRAINT fk_assignments_session
                                 FOREIGN KEY (exam_session_id)
                                     REFERENCES exam_sessions(id),

                             CONSTRAINT fk_assignments_group
                                 FOREIGN KEY (exam_group_id)
                                     REFERENCES exam_groups(id)
) ENGINE = InnoDB;


-- 10. Predaje rešenja
CREATE TABLE submissions (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             exam_attempt_id BIGINT NOT NULL,
                             assignment_id BIGINT NOT NULL,
                             kind VARCHAR(20) NOT NULL,
                             original_file_name VARCHAR(255) NOT NULL,
                             storage_key VARCHAR(500) NOT NULL,
                             submitted_at DATETIME(6) NOT NULL,

                             PRIMARY KEY (id),

                             CONSTRAINT uk_submissions_storage_key
                                 UNIQUE (storage_key),

                             CONSTRAINT fk_submissions_attempt
                                 FOREIGN KEY (exam_attempt_id)
                                     REFERENCES exam_attempts(id),

                             CONSTRAINT fk_submissions_assignment
                                 FOREIGN KEY (assignment_id)
                                     REFERENCES assignments(id)
) ENGINE = InnoDB;


-- 11. Komentari na kod
CREATE TABLE code_comments (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               submission_id BIGINT NOT NULL,
                               author_id BIGINT NOT NULL,
                               file_path VARCHAR(500) NOT NULL,
                               line_number INT NOT NULL,
                               content LONGTEXT NOT NULL,
                               created_at DATETIME(6) NOT NULL,

                               PRIMARY KEY (id),

                               CONSTRAINT fk_comments_submission
                                   FOREIGN KEY (submission_id)
                                       REFERENCES submissions(id),

                               CONSTRAINT fk_comments_author
                                   FOREIGN KEY (author_id)
                                       REFERENCES app_users(id),

                               CONSTRAINT chk_comment_line
                                   CHECK (line_number > 0)
) ENGINE = InnoDB;


-- 12. Ocene
CREATE TABLE grades (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        exam_attempt_id BIGINT NOT NULL,
                        graded_by_user_id BIGINT NOT NULL,
                        points DECIMAL(7, 2) NOT NULL,
                        feedback LONGTEXT,
                        graded_at DATETIME(6) NOT NULL,

                        PRIMARY KEY (id),

                        CONSTRAINT uk_grades_attempt
                            UNIQUE (exam_attempt_id),

                        CONSTRAINT fk_grades_attempt
                            FOREIGN KEY (exam_attempt_id)
                                REFERENCES exam_attempts(id),

                        CONSTRAINT fk_grades_teacher
                            FOREIGN KEY (graded_by_user_id)
                                REFERENCES app_users(id),

                        CONSTRAINT chk_grade_points
                            CHECK (points >= 0)
) ENGINE = InnoDB;