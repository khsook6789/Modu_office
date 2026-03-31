import { Component, type ReactNode } from 'react';

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

export default class ErrorBoundary extends Component<Props, State> {
    state: State = { hasError: false, error: null };

    static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, info: { componentStack: string }) {
        console.error('[ErrorBoundary]', error, info.componentStack);
    }

    render() {
        if (this.state.hasError) {
            return (
                <div style={{
                    display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                    minHeight: '60vh', padding: '2rem', textAlign: 'center'
                }}>
                    <p style={{ fontSize: '3rem', marginBottom: '1rem' }}>⚠️</p>
                    <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a', marginBottom: '0.5rem' }}>
                        문제가 발생했습니다
                    </h2>
                    <p style={{ color: '#64748b', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
                        {this.state.error?.message || '예기치 않은 오류가 발생했습니다.'}
                    </p>
                    <button
                        onClick={() => { this.setState({ hasError: false, error: null }); window.location.href = '/'; }}
                        style={{
                            background: 'var(--color-primary)', color: '#fff', border: 'none',
                            borderRadius: '0.75rem', padding: '0.625rem 1.5rem',
                            fontWeight: 600, fontSize: '0.9rem', cursor: 'pointer'
                        }}
                    >
                        홈으로 돌아가기
                    </button>
                </div>
            );
        }
        return this.props.children;
    }
}
