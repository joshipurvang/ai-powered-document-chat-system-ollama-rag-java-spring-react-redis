// src/services/api.js
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 120000, // 2 minutes for document processing
});

/**
 * API Service for Document Chat AI
 * Provides methods to interact with backend REST APIs
 */
const api = {
  /**
   * Creates a new user session
   * @param {Object} userInfo - User information (name, phoneNumber, deviceInfo)
   * @returns {Promise} API response with session data
   */
  createSession: async (userInfo) => {
    return await apiClient.post('/session/create', userInfo);
  },

  /**
   * Retrieves session by ID
   * @param {string} sessionId - Session identifier
   * @returns {Promise} API response with session data
   */
  getSession: async (sessionId) => {
    return await apiClient.get(`/session/${sessionId}`);
  },

  /**
   * Ends a user session
   * @param {string} sessionId - Session identifier
   * @returns {Promise} API response
   */
  endSession: async (sessionId) => {
    return await apiClient.post(`/session/${sessionId}/end`);
  },

  /**
   * Uploads documents for processing
   * @param {string} sessionId - Session identifier
   * @param {FormData} formData - Form data containing files
   * @param {Function} onUploadProgress - Progress callback
   * @returns {Promise} API response with upload results
   */
  uploadDocuments: async (sessionId, formData, onUploadProgress) => {
    formData.append('sessionId', sessionId);
    
    return await apiClient.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        );
        if (onUploadProgress) {
          onUploadProgress(percentCompleted);
        }
      },
    });
  },

  /**
   * Gets documents for a session
   * @param {string} sessionId - Session identifier
   * @returns {Promise} API response with document list
   */
  getDocuments: async (sessionId) => {
    return await apiClient.get(`/documents/${sessionId}`);
  },

  /**
   * Clears all documents for a session
   * @param {string} sessionId - Session identifier
   * @returns {Promise} API response
   */
  clearDocuments: async (sessionId) => {
    return await apiClient.delete(`/documents/${sessionId}`);
  },

  /**
   * Sends a query to chat with documents
   * @param {string} sessionId - Session identifier
   * @param {string} question - User question
   * @returns {Promise} API response with answer
   */
  sendQuery: async (sessionId, question) => {
    return await apiClient.post('/chat/query', {
      sessionId,
      question,
    });
  },

  /**
   * Gets chat history for a session
   * @param {string} sessionId - Session identifier
   * @returns {Promise} API response with chat history
   */
  getChatHistory: async (sessionId) => {
    return await apiClient.get(`/chat/${sessionId}/history`);
  },

  /**
   * Exports chat history as text file
   * @param {string} sessionId - Session identifier
   * @returns {Promise} API response with text content
   */
  exportChatHistory: async (sessionId) => {
    return await apiClient.get(`/chat/${sessionId}/export`, {
      responseType: 'text',
    });
  },
};

// Add response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error);
    
    if (error.response) {
      // Server responded with error status
      const message = error.response.data?.error || 
                     error.response.data?.message || 
                     'An error occurred';
      return Promise.reject(new Error(message));
    } else if (error.request) {
      // Request made but no response
      return Promise.reject(new Error('No response from server. Please check your connection.'));
    } else {
      // Error in request setup
      return Promise.reject(error);
    }
  }
);

export default api;