const API_BASE_URL = '/api';

const parseResponseBody = async (response) => {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    const error = new Error('서버 응답을 처리할 수 없습니다.');
    error.status = response.status;
    error.code = 'INVALID_API_RESPONSE';
    throw error;
  }
};

export const apiFetch = async (endpoint, options = {}) => {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  const config = {
    ...options,
    headers,
  };

  try {
    const response = await fetch(url, config);
    const data = await parseResponseBody(response);

    if (!response.ok) {
      const error = new Error(
        data?.error?.message || data?.message || '요청을 처리하지 못했습니다.',
      );
      error.status = response.status;
      error.code = data?.error?.code || 'API_REQUEST_FAILED';
      error.fields = data?.error?.fields || [];
      error.requestId = data?.error?.requestId || null;
      throw error;
    }

    if (!data) {
      const error = new Error('서버 응답을 처리할 수 없습니다.');
      error.status = response.status;
      error.code = 'INVALID_API_RESPONSE';
      throw error;
    }

    return data;
  } catch (error) {
    throw error;
  }
};

export const authApi = {
  signup: (data) => apiFetch('/auth/signup', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  login: (data) => apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  refresh: (refreshToken) => apiFetch('/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  }),
  logout: (refreshToken) => apiFetch('/auth/logout', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  }),
};

export const lostItemApi = {
  createSearch: async (request, accessToken) => {
    if (!accessToken) {
      const error = new Error('로그인이 필요합니다.');
      error.status = 401;
      error.code = 'AUTH_REQUIRED';
      throw error;
    }

    const response = await apiFetch('/lost-items', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(request),
    });

    if (!response.success || !response.data) {
      const error = new Error('검색 결과를 처리할 수 없습니다.');
      error.code = 'INVALID_API_RESPONSE';
      throw error;
    }

    return response.data;
  },

  getDetail: async (lostItemId, accessToken) => {
    if (!accessToken) {
      const error = new Error('로그인이 필요합니다.');
      error.status = 401;
      error.code = 'AUTH_REQUIRED';
      throw error;
    }

    const response = await apiFetch(`/lost-items/${lostItemId}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!response.success || !response.data) {
      const error = new Error('분실물 상세 정보를 불러올 수 없습니다.');
      error.code = 'INVALID_API_RESPONSE';
      throw error;
    }

    return response.data;
  },

  runSearch: async (lostItemId, request, accessToken) => {
    if (!accessToken) {
      const error = new Error('로그인이 필요합니다.');
      error.status = 401;
      error.code = 'AUTH_REQUIRED';
      throw error;
    }

    const response = await apiFetch(`/lost-items/${lostItemId}/searches`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(request),
    });

    if (!response.success || !response.data) {
      const error = new Error('조건 보정 검색 결과를 처리할 수 없습니다.');
      error.code = 'INVALID_API_RESPONSE';
      throw error;
    }

    return response.data;
  },
};

export const memberApi = {
  updateLanguage: async (languageCode, accessToken) => {
    if (!accessToken) {
      const error = new Error('로그인이 필요합니다.');
      error.status = 401;
      error.code = 'AUTH_REQUIRED';
      throw error;
    }

    const response = await apiFetch('/members/me', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({ languageCode }),
    });

    // 프로젝트에서 성공 여부를 어떻게 내려주는지 모르지만, 일단 에러 안나면 성공
    return response;
  }
};
