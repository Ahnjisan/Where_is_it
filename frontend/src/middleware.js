import { NextResponse } from 'next/server';

export function middleware(request) {
  const token = request.cookies.get('where_access_token')?.value;
  const { pathname } = request.nextUrl;

  const isAuthPage = pathname === '/signin' || pathname === '/signup' || pathname === '/login';
  const isRoot = pathname === '/';

  // If user is not authenticated
  if (!token) {
    // Redirect to signin if not root or auth page
    if (!isRoot && !isAuthPage) {
      return NextResponse.redirect(new URL('/signin', request.url));
    }
  } else {
    // If user is authenticated, prevent access to auth pages
    if (isAuthPage) {
      return NextResponse.redirect(new URL('/', request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico, sitemap.xml, robots.txt (metadata files)
     * - icon.svg, icon.png (icons)
     */
    '/((?!api|_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt|icon.svg|icon.png).*)',
  ],
};
