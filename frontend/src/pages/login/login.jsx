import { useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { api } from '../../config/api';
import './login.css';

export default function Login() {
    const [role, setRole] = useState('teacher');
    const [showPw, setShowPw] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);

    const navigate = useNavigate();
    const { login } = useAuth();

    const emailRef = useRef(null);
    const pwRef = useRef(null);

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const emailErr = useMemo(() => {
        if (!email) return 'Email is required.';
        if (!/\S+@\S+\.\S+/.test(email)) return 'Enter a valid email.';
        return null;
    }, [email]);

    const pwErr = useMemo(() => {
        if (!password) return 'Password is required.';
        if (password.length < 8) return 'Use at least 8 characters.';
        return null;
    }, [password]);

    async function handleSubmit(e) {
        e.preventDefault();
        setError(null);

        // COMMENTED OUT: Form validation checks to allow testing without input
        // if (emailErr) { emailRef.current?.focus(); return; }
        // if (pwErr) { pwRef.current?.focus(); return; }

        setSubmitting(true);

        // TODO: Add authentication functionality
        // For now, just simulate a successful login without actual authentication

        /* COMMENTED OUT: Original authentication logic
        try {
            // Try backend authentication first
            const res = await fetch(api.auth.login(), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email, password, role })
            });
            
            if (res.ok) {
                // Backend login successful
                const userData = await res.json();
                await login({
                    email: userData.email || email,
                    name: userData.name || email.split('@')[0],
                    role: role === 'teacher' ? 'Teacher' : 'TA'
                });
                navigate('/lab-selector');
            } else {
                throw new Error('Login failed');
            }
        } catch (err) {
            // Fallback to demo login if backend fails
            try {
                await login({
                    email,
                    name: email.split('@')[0],
                    role: role === 'teacher' ? 'Teacher' : 'TA'
                });
                navigate('/lab-selector');
            } catch (loginErr) {
                setError(loginErr.message || 'Login failed');
            }
        } finally {
            setSubmitting(false);
        }
        */

        // Simulate loading time
        setTimeout(async () => {
            // Set a demo user for navigation purposes
            try {
                await login({
                    email: email || 'demo@example.com',
                    name: 'Demo User',
                    role: role === 'teacher' ? 'Teacher' : 'TA'
                });
                setSubmitting(false);
                console.log('Form submitted:', { email, password, role });
                // Navigate to lab selector page
                navigate('/lab-selector');
            } catch (error) {
                setSubmitting(false);
                console.error('Demo login failed:', error);
            }
        }, 1000);
    }

    return (
        <main className="center">
            <section className="card" aria-labelledby="login-title">
                <h1 id="login-title" className="h1">Login</h1>
                <p className="subtle">For Teachers and Teaching Assistants</p>

                {error && <div role="alert" className="error">{error}</div>}

                <form onSubmit={handleSubmit} noValidate>
                    <div className="field">
                        <label htmlFor="email" className="label">Email</label>
                        <input
                            ref={emailRef}
                            id="email"
                            name="email"
                            type="email"
                            className="input"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            aria-invalid={!!emailErr}
                            aria-describedby={emailErr ? 'email-err' : undefined}
                            placeholder="name@school.edu"
                            required
                        />
                        {emailErr && <div id="email-err" className="error">{emailErr}</div>}
                    </div>

                    <div className="field">
                        <label htmlFor="password" className="label">Password</label>
                        <div className="password-container">
                            <input
                                ref={pwRef}
                                id="password"
                                name="password"
                                type={showPw ? 'text' : 'password'}
                                className="input"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                aria-invalid={!!pwErr}
                                aria-describedby={pwErr ? 'password-err' : undefined}
                                placeholder="••••••••"
                                required
                            />
                            <button
                                type="button"
                                onClick={() => setShowPw(s => !s)}
                                aria-pressed={showPw}
                                className="password-toggle"
                            >
                                {showPw ? 'Hide' : 'Show'}
                            </button>
                        </div>
                        {pwErr && <div id="password-err" className="error">{pwErr}</div>}
                    </div>

                    <div className="field">
                        <label htmlFor="role" className="label">Role</label>
                        <select
                            id="role"
                            className="select"
                            value={role}
                            onChange={(e) => setRole(e.target.value)}
                        >
                            <option value="teacher">Teacher</option>
                            <option value="ta">Teaching Assistant</option>
                        </select>
                    </div>

                    <div className="actions">
                        <button className="button" type="submit" disabled={submitting}>
                            {submitting ? 'Signing in…' : 'Sign in'}
                        </button>
                    </div>
                </form>
            </section>
            <div style={{
                textAlign: 'center',
                marginTop: '32px',
                maxWidth: '440px'
            }}>
                <h1 style={{
                    fontSize: '32px',
                    fontWeight: '700',
                    color: '#0f172a',
                    margin: '0 0 12px 0',
                    lineHeight: '1.2'
                }}>
                    Lab Sign-Off
                </h1>
                <p style={{
                    fontSize: '16px',
                    color: '#64748b',
                    margin: '0',
                    lineHeight: '1.5'
                }}>
                    Streamline lab checkpoint management and student progress tracking for educational institutions
                </p>
            </div>
        </main>
    );
}
