import { useMemo, useRef, useState } from 'react';

export default function Login() {
    const [role, setRole] = useState('teacher');
    const [showPw, setShowPw] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);

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

        if (emailErr) { emailRef.current?.focus(); return; }
        if (pwErr) { pwRef.current?.focus(); return; }

        setSubmitting(true);
        try {
            // Swap this with your real call. If you use cookie sessions in Canvas (iframe),
            // your server must set cookies with SameSite=None; Secure and you should pass credentials: 'include'.
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email, password, role })
            });
            if (!res.ok) throw new Error('Login failed');
            window.location.assign('/dashboard');
        } catch (err) {
            setError(err.message || 'Something went wrong.');
        } finally {
            setSubmitting(false);
        }
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
                        <div style={{ position: 'relative' }}>
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
                                style={{
                                    position: 'absolute', right: 6, top: 6, height: 32,
                                    padding: '0 10px', borderRadius: 8, border: '1px solid #cbd5e1',
                                    background: '#f8fafc', cursor: 'pointer'
                                }}
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
                        <button className="button" type="submit" disabled={submitting || !!emailErr || !!pwErr}>
                            {submitting ? 'Signing in…' : 'Sign in'}
                        </button>
                    </div>
                </form>
            </section>
        </main>
    );
}
