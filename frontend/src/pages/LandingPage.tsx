import { Link } from "react-router-dom";

export function LandingPage() {
    return (
        <div className="flex flex-col min-h-screen">
            {/* Header */}
            <header className="px-4 lg:px-6 h-16 flex items-center border-b bg-white sticky top-0 z-50 shadow-sm">
                <Link to="/" className="flex items-center justify-center">
                    <span className="text-2xl font-bold text-slate-900">RefRoster</span>
                </Link>
                <nav className="ml-auto flex items-center gap-4 sm:gap-6">
                    <Link
                        to="/login"
                        className="text-sm font-medium hover:text-blue-600 transition-colors"
                    >
                        Login
                    </Link>
                    <Link
                        to="/register"
                        className="text-sm font-medium px-4 py-2 bg-slate-900 text-white rounded-md hover:bg-slate-800 transition-colors"
                    >
                        Register
                    </Link>
                </nav>
            </header>

            <main className="flex-1">
                {/* Hero Section */}
                <section className="w-full py-12 md:py-24 lg:py-32 xl:py-48 bg-slate-50">
                    <div className="container px-4 md:px-6 mx-auto">
                        <div className="flex flex-col items-center space-y-4 text-center">
                            <div className="space-y-2">
                                <h1 className="text-3xl font-bold tracking-tighter sm:text-4xl md:text-5xl lg:text-6xl/none">
                                    Simplify Referee Scheduling
                                </h1>
                                <p className="mx-auto max-w-[700px] text-slate-500 md:text-xl">
                                    The ultimate platform for sports organizations to manage games,
                                    availability, and assignments with ease.
                                </p>
                            </div>
                            <div className="space-x-4">
                                <Link
                                    to="/register"
                                    className="inline-flex h-11 items-center justify-center rounded-md bg-blue-600 px-8 text-sm font-medium text-white shadow transition-colors hover:bg-blue-700 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-blue-700"
                                >
                                    Get Started
                                </Link>
                            </div>
                        </div>
                    </div>
                </section>

                {/* Features Grid */}
                <section className="w-full py-12 md:py-24 lg:py-32 bg-white">
                    <div className="container px-4 md:px-6 mx-auto">
                        <div className="grid gap-10 sm:grid-cols-2 md:grid-cols-3">
                            <div className="flex flex-col items-center space-y-4 text-center">
                                <div className="p-4 bg-slate-50 rounded-full shadow-sm">
                                    <svg
                                        className="h-6 w-6 text-blue-600"
                                        fill="none"
                                        height="24"
                                        stroke="currentColor"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth="2"
                                        viewBox="0 0 24 24"
                                        width="24"
                                        xmlns="http://www.w3.org/2000/svg"
                                    >
                                        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                                        <circle cx="9" cy="7" r="4" />
                                        <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
                                        <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                                    </svg>
                                </div>
                                <h2 className="text-xl font-bold">
                                    For Admins: Effortless scheduling
                                </h2>
                                <p className="text-slate-500">
                                    Create games, track availability, and assign referees in just a
                                    few clicks. Keep your league running smoothly.
                                </p>
                            </div>
                            <div className="flex flex-col items-center space-y-4 text-center">
                                <div className="p-4 bg-slate-50 rounded-full shadow-sm">
                                    <svg
                                        className="h-6 w-6 text-blue-600"
                                        fill="none"
                                        height="24"
                                        stroke="currentColor"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth="2"
                                        viewBox="0 0 24 24"
                                        width="24"
                                        xmlns="http://www.w3.org/2000/svg"
                                    >
                                        <rect height="18" rx="2" ry="2" width="18" x="3" y="4" />
                                        <line x1="16" x2="16" y1="2" y2="6" />
                                        <line x1="8" x2="8" y1="2" y2="6" />
                                        <line x1="3" x2="21" y1="10" y2="10" />
                                    </svg>
                                </div>
                                <h2 className="text-xl font-bold">
                                    For Referees: Manage your time
                                </h2>
                                <p className="text-slate-500">
                                    Submit your availability and view your assignments on the go.
                                    Mobile-first design for referees on the field.
                                </p>
                            </div>
                            <div className="flex flex-col items-center space-y-4 text-center">
                                <div className="p-4 bg-slate-50 rounded-full shadow-sm">
                                    <svg
                                        className="h-6 w-6 text-blue-600"
                                        fill="none"
                                        height="24"
                                        stroke="currentColor"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth="2"
                                        viewBox="0 0 24 24"
                                        width="24"
                                        xmlns="http://www.w3.org/2000/svg"
                                    >
                                        <path d="m8 3 4 8 5-5 5 15H2L8 3z" />
                                    </svg>
                                </div>
                                <h2 className="text-xl font-bold">Seamless Integration</h2>
                                <p className="text-slate-500">
                                    Real-time updates and notifications ensure everyone is on the
                                    same page. No more missed games or scheduling conflicts.
                                </p>
                            </div>
                        </div>
                    </div>
                </section>
            </main>

            {/* Footer */}
            <footer className="flex flex-col gap-2 sm:flex-row py-6 w-full shrink-0 items-center px-4 md:px-6 border-t bg-slate-50">
                <p className="text-xs text-slate-500">© 2026 RefRoster Inc. All rights reserved.</p>
                <nav className="sm:ml-auto flex gap-4 sm:gap-6">
                    <Link to="#" className="text-xs hover:underline underline-offset-4">
                        Terms of Service
                    </Link>
                    <Link to="#" className="text-xs hover:underline underline-offset-4">
                        Privacy
                    </Link>
                </nav>
            </footer>
        </div>
    );
}
