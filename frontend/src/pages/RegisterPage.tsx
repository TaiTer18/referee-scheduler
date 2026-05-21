import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { registerUser, validateJoinCode } from "../api/auth";
import { useAuthStore } from "../store/auth-store";
import { Check } from "lucide-react";

const JOIN_CODE_LENGTH = 8;

const registerSchema = z
    .object({
        role: z.enum(["ADMIN", "REFEREE"]),
        fullName: z.string().min(2, "Enter your full name"),
        email: z.string().email("Enter a valid email address"),
        phoneNumber: z.string().optional(),
        password: z.string().min(8, "Password must be at least 8 characters"),
        organizationName: z.string().optional(),
        joinCode: z.string().optional(),
    })
    .superRefine((values, context) => {
        if (values.role === "ADMIN" && !values.organizationName?.trim()) {
            context.addIssue({
                code: "custom",
                path: ["organizationName"],
                message: "Enter your organization's name",
            });
        }

        if (values.role === "REFEREE" && !values.joinCode?.trim()) {
            context.addIssue({
                code: "custom",
                path: ["joinCode"],
                message: "Enter a join code from your assignor",
            });
        }

        if (
            values.role === "REFEREE" &&
            values.joinCode &&
            values.joinCode.trim().length !== JOIN_CODE_LENGTH
        ) {
            context.addIssue({
                code: "custom",
                path: ["joinCode"],
                message: `Join code must be exactly ${JOIN_CODE_LENGTH} characters long`,
            });
        }
    });

type RegisterFormValues = z.infer<typeof registerSchema>;
type JoinCodeStatus = "idle" | "checking" | "valid" | "invalid";

export function RegisterPage() {
    const navigate = useNavigate();
    const setSession = useAuthStore((state) => state.setSession);
    const [showPassword, setShowPassword] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [joinCodeStatus, setJoinCodeStatus] = useState<JoinCodeStatus>("idle");
    const [joinCodeOrganizationName, setJoinCodeOrganizationName] = useState<string | null>(null);
    const {
        register,
        handleSubmit,
        watch,
        resetField,
        clearErrors,
        formState: { errors, isSubmitting },
    } = useForm<RegisterFormValues>({
        resolver: zodResolver(registerSchema),
        defaultValues: {
            role: "REFEREE",
            fullName: "",
            email: "",
            password: "",
            phoneNumber: "",
            organizationName: "",
            joinCode: "",
        },
    });

    const selectedRole = watch("role");
    const joinCodeValue = watch("joinCode");

    useEffect(() => {
        if (selectedRole === "ADMIN") {
            resetField("joinCode");
            clearErrors("joinCode");
            setJoinCodeStatus("idle");
            setJoinCodeOrganizationName(null);
        }

        if (selectedRole === "REFEREE") {
            resetField("organizationName");
            clearErrors("organizationName");
        }
    }, [selectedRole, resetField, clearErrors]);

    useEffect(() => {
        if (selectedRole !== "REFEREE") {
            setJoinCodeStatus("idle");
            setJoinCodeOrganizationName(null);
            return;
        }

        const normalizedJoinCode = joinCodeValue?.trim().toUpperCase() ?? "";

        if (normalizedJoinCode.length !== JOIN_CODE_LENGTH) {
            setJoinCodeStatus("idle");
            setJoinCodeOrganizationName(null);
            return;
        }

        let cancelled = false;
        const timeoutId = window.setTimeout(async () => {
            setJoinCodeStatus("checking");

            try {
                const result = await validateJoinCode(normalizedJoinCode);

                if (cancelled) {
                    return;
                }

                setJoinCodeStatus(result.valid ? "valid" : "invalid");
                setJoinCodeOrganizationName(result.organizationName);
            } catch {
                if (!cancelled) {
                    setJoinCodeStatus("invalid");
                    setJoinCodeOrganizationName(null);
                }
            }
        }, 350);

        return () => {
            cancelled = true;
            window.clearTimeout(timeoutId);
        };
    }, [joinCodeValue, selectedRole]);

    async function onSubmit(values: RegisterFormValues) {
        setSubmitError(null);

        if (values.role === "REFEREE" && joinCodeStatus === "invalid") {
            setSubmitError("That join code is not connected to an active organization.");
            return;
        }

        try {
            const response = await registerUser({
                email: values.email.trim(),
                password: values.password,
                fullName: values.fullName.trim(),
                role: values.role,
                phoneNumber: values.phoneNumber?.trim() || undefined,
                organizationName:
                    values.role === "ADMIN" ? values.organizationName?.trim() : undefined,
                joinCode:
                    values.role === "REFEREE" ? values.joinCode?.trim().toUpperCase() : undefined,
            });

            setSession(response);
            navigate("/app", { replace: true });
        } catch (error) {
            setSubmitError(error instanceof Error ? error.message : "Registration failed.");
        }
    }

    return (
        <main className="min-h-screen bg-slate-50 px-4 py-10">
            <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 lg:grid lg:grid-cols-[0.9fr_1.1fr] lg:items-start">
                <section className="pt-4 lg:pt-12">
                    <Link to="/" className="text-2xl font-bold text-slate-900">
                        RefRoster
                    </Link>

                    <div className="mt-10 max-w-md">
                        <p className="text-sm font-semibold uppercase tracking-wide text-blue-600">
                            Create your account
                        </p>
                        <h1 className="mt-3 text-4xl font-bold tracking-tight text-slate-950">
                            Start organizing games and referee availability.
                        </h1>
                        <p className="mt-4 text-base leading-7 text-slate-600">
                            Set up your organization as an admin, or join an existing team as a
                            referee.
                        </p>
                    </div>
                </section>

                <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
                    <div>
                        <h2 className="text-2xl font-semibold text-slate-950">Register</h2>
                        <p className="mt-2 text-sm text-slate-500">
                            Choose your role and enter your account details.
                        </p>
                    </div>

                    <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
                        <div>
                            <span className="block text-sm font-medium text-slate-700">
                                I am registering as
                            </span>

                            <div className="mt-2 grid grid-cols-2 rounded-md border border-slate-200 bg-slate-100 p-1">
                                <label
                                    className={`cursor-pointer rounded px-3 py-2 text-center text-sm font-medium transition ${
                                        selectedRole === "REFEREE"
                                            ? "bg-white text-slate-950 shadow-sm"
                                            : "text-slate-500 hover:text-slate-800"
                                    }`}
                                >
                                    <input
                                        className="sr-only"
                                        type="radio"
                                        value="REFEREE"
                                        {...register("role")}
                                    />
                                    Referee
                                </label>

                                <label
                                    className={`cursor-pointer rounded px-3 py-2 text-center text-sm font-medium transition ${
                                        selectedRole === "ADMIN"
                                            ? "bg-white text-slate-950 shadow-sm"
                                            : "text-slate-500 hover:text-slate-800"
                                    }`}
                                >
                                    <input
                                        className="sr-only"
                                        type="radio"
                                        value="ADMIN"
                                        {...register("role")}
                                    />
                                    Assignor
                                </label>
                            </div>
                        </div>
                        <div className="grid gap-5 sm:grid-cols-2">
                            <div className="sm:col-span-2">
                                <label
                                    className="block text-sm font-medium text-slate-700"
                                    htmlFor="fullName"
                                >
                                    Full name
                                </label>
                                <input
                                    id="fullName"
                                    autoComplete="name"
                                    className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
                                    placeholder="Jordan Lee"
                                    {...register("fullName")}
                                />
                                {errors.fullName ? (
                                    <p className="mt-1 text-sm text-rose-600">
                                        {errors.fullName.message}
                                    </p>
                                ) : null}
                            </div>

                            <div className="sm:col-span-2">
                                <label
                                    className="block text-sm font-medium text-slate-700"
                                    htmlFor="email"
                                >
                                    Email
                                </label>
                                <input
                                    id="email"
                                    type="email"
                                    autoComplete="email"
                                    className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
                                    placeholder="you@example.com"
                                    {...register("email")}
                                />
                                {errors.email ? (
                                    <p className="mt-1 text-sm text-rose-600">
                                        {errors.email.message}
                                    </p>
                                ) : null}
                            </div>
                        </div>
                        <div className="sm:col-span-2">
                            <label
                                className="block text-sm font-medium text-slate-700"
                                htmlFor="phoneNumber"
                            >
                                Phone number
                            </label>
                            <input
                                id="phoneNumber"
                                type="tel"
                                autoComplete="tel"
                                className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
                                placeholder="123-456-7890"
                                {...register("phoneNumber")}
                            />
                            {errors.phoneNumber ? (
                                <p className="mt-1 text-sm text-rose-600">
                                    {errors.phoneNumber.message}
                                </p>
                            ) : null}
                        </div>
                        <div>
                            <label
                                className="block text-sm font-medium text-slate-700"
                                htmlFor="password"
                            >
                                Password
                            </label>

                            <div className="mt-2 flex rounded-md border border-slate-300 bg-white focus-within:border-blue-600 focus-within:ring-2 focus-within:ring-blue-100">
                                <input
                                    id="password"
                                    type={showPassword ? "text" : "password"}
                                    autoComplete="new-password"
                                    className="min-w-0 flex-1 rounded-l-md border-0 bg-transparent px-3 py-2 text-slate-950 outline-none"
                                    placeholder="At least 8 characters"
                                    {...register("password")}
                                />

                                <button
                                    type="button"
                                    className="shrink-0 rounded-r-md px-3 text-sm font-medium text-blue-600 hover:text-blue-700"
                                    onClick={() => setShowPassword((current) => !current)}
                                >
                                    {showPassword ? "Hide" : "Show"}
                                </button>
                            </div>

                            {errors.password ? (
                                <p className="mt-1 text-sm text-rose-600">
                                    {errors.password.message}
                                </p>
                            ) : null}
                        </div>
                        {selectedRole === "ADMIN" ? (
                            <div>
                                <label
                                    className="block text-sm font-medium text-slate-700"
                                    htmlFor="organizationName"
                                >
                                    Organization name
                                </label>
                                <input
                                    id="organizationName"
                                    className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
                                    placeholder="Vancouver Soccer Club"
                                    {...register("organizationName")}
                                />
                                {errors.organizationName ? (
                                    <p className="mt-1 text-sm text-rose-600">
                                        {errors.organizationName.message}
                                    </p>
                                ) : null}
                            </div>
                        ) : (
                            <div>
                                <label
                                    className="block text-sm font-medium text-slate-700"
                                    htmlFor="joinCode"
                                >
                                    Join code
                                </label>
                                <div className="relative mt-2">
                                    <input
                                        id="joinCode"
                                        className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 uppercase tracking-wide text-slate-950 outline-none transition focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
                                        placeholder="ABC12345"
                                        autoCapitalize="characters"
                                        maxLength={JOIN_CODE_LENGTH}
                                        {...register("joinCode")}
                                    />
                                    {joinCodeStatus === "valid" ? (
                                        <Check className="absolute right-3 top-1/2 -translate-y-1/2 text-green-600" />
                                    ) : null}
                                </div>

                                <p className="mt-2 text-sm text-slate-500">
                                    Don&apos;t have a join code? Ask your assignor.
                                </p>
                                {joinCodeStatus === "checking" ? (
                                    <p className="mt-1 text-sm text-blue-600">
                                        Checking join code...
                                    </p>
                                ) : null}
                                {joinCodeStatus === "invalid" ? (
                                    <p className="mt-1 text-sm text-rose-600">
                                        No active organization was found for that join code.
                                    </p>
                                ) : null}
                                {errors.joinCode ? (
                                    <p className="mt-1 text-sm text-rose-600">
                                        {errors.joinCode.message}
                                    </p>
                                ) : null}
                            </div>
                        )}
                        {submitError ? (
                            <p className="rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700">
                                {submitError}
                            </p>
                        ) : null}
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="flex w-full items-center justify-center rounded-md bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-70"
                        >
                            {isSubmitting ? "Creating account..." : "Create account"}
                        </button>

                        <p className="text-center text-sm text-slate-500">
                            Already have an account?{" "}
                            <Link
                                to="/login"
                                className="font-medium text-blue-600 hover:text-blue-700"
                            >
                                Sign in
                            </Link>
                        </p>
                    </form>
                </section>
            </div>
        </main>
    );
}
