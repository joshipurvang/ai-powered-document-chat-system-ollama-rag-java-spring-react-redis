# ai-powered-document-chat-system-ollama-rag-spring-react-redis
# Document Chat AI System 

A comprehensive RAG (Retrieval Augmented Generation) based document chat system that allows users to upload documents (PDF, DOCX, PPTX, TXT) and interact with them using AI-powered natural language queries.

## Features

### Core Functionality
- **Multi-Format Support**: PDF, DOCX, PPTX, and TXT files
- **Smart Document Processing**: Intelligent text extraction and chunking
- **RAG Pipeline**: Retrieval Augmented Generation for accurate answers
- **Real-time Chat**: WebSocket-based instant messaging
- **Session Management**: Secure user sessions with Redis
- **Conversation Logging**: Complete audit trail of all interactions
- **Export Functionality**: Download chat history as text files
- **No Hallucinations**: AI responds only based on uploaded documents

### Technical Features
- **Vector Database**: Redis for efficient semantic search
- **Embeddings**: Nomic-embed-text model for document embeddings
- **LLM**: Llama 3.2 or Mistral for question answering
- **Responsive UI**: Mobile-friendly Bootstrap 5 design
- **Real-time Updates**: WebSocket integration for live chat
- **Location Tracking**: Captures user location and device info
- **API Documentation**: Auto-generated Swagger/OpenAPI docs

##  Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React)                        │
│  ┌──────────────┬──────────────┬──────────────────────┐     │
│  │ User Info    │ Doc Upload   │ Chat Interface       │     │
│  │ Form         │ Component    │ (WebSocket)          │     │
│  └──────────────┴──────────────┴──────────────────────┘     │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/WebSocket
┌────────────────────────▼────────────────────────────────────┐
│                  Backend (Spring Boot)                      │
│  ┌──────────────┬──────────────┬──────────────────────┐     │
│  │ REST APIs    │ WebSocket    │ RAG Service          │     │
│  │              │ Handler      │                      │     │
│  └──────────────┴──────────────┴──────────────────────┘     │
│  ┌──────────────┬──────────────┬──────────────────────┐     │
│  │ Document     │ Session      │ Logging              │     │
│  │ Processing   │ Service      │ Service              │     │
│  └──────────────┴──────────────┴──────────────────────┘     │
└────────┬────────────┬────────────────┬──────────────────────┘
         │            │                │
    ┌────▼────┐  ┌────▼────┐     ┌─────▼────┐
    │ Redis   │  │ Ollama  │     │ File     │
    │ Vector  │  │ LLM     │     │ Storage  │
    │ Store   │  │         │     │          │
    └─────────┘  └─────────┘     └──────────┘
```

## Technology Stack

### Backend
- **Java 21**: Latest LTS version
- **Spring Boot 3.2.1**: Web framework
- **Spring AI 1.0.0-M4**: AI integration
- **Ollama**: Local LLM deployment
- **Redis**: Vector database and caching
- **Apache PDFBox**: PDF processing
- **Apache POI**: Office document processing
- **Apache Tika**: Universal document parser

### Frontend
- **React 18.2**: UI framework
- **Bootstrap 5.3**: CSS framework
- **Axios**: HTTP client
- **SockJS + STOMP**: WebSocket client
- **React Toastify**: Notifications
- **React Icons**: Icon library

### AI Models
- **Embeddings**: nomic-embed-text (384 dimensions)
- **Q&A Model**: llama3.2:7b or mistral:7b
- **Lightweight Option**: llama3.2:3b

##  Prerequisites

- **Java 21** or higher
- **Node.js 18** or higher
- **Maven 3.8+**
- **Redis 7.0+**
- **Ollama** (installed locally or on OCI)
- **8GB RAM minimum** (12GB recommended for llama3.1:8b)

##  Quick Start

### 1. Install Ollama

```bash
# Linux/macOS
curl -fsSL https://ollama.com/install.sh | sh

# Pull required models
ollama pull nomic-embed-text
ollama pull llama3.1:8b  # or llama3.2:3b for lower memory
```

### 2. Start Redis

```bash
# Using Docker
docker run -d -p 6379:6379 redis:latest


### 3. Backend Setup

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend will start on `http://localhost:8080`

### 4. Frontend Setup

```bash
cd frontend
npm install
npm start
```

Frontend will start on `http://localhost:3000`

### 5. Access the Application

Open your browser and navigate to:
- **Application**: http://localhost:3000
- **API Docs**: http://localhost:8080/api/swagger-ui.html
- **Health Check**: http://localhost:8080/api/actuator/health

## Usage Guide

### Step 1: User Registration
1. Enter your full name
2. Provide phone number (10-15 digits)
3. Optionally add device information
4. Click "Start Session"

### Step 2: Document Upload
1. Click "Choose Files" to select documents
2. Upload up to 3 files (max 5MB each)
3. Supported formats: PDF, DOCX, PPTX, TXT
4. Click "Upload & Process Documents"
5. Wait for processing to complete

### Step 3: Chat with Documents
1. Type your question in the chat input
2. Press Enter or click Send button
3. AI will respond based on document content
4. Continue asking questions as needed
5. Export chat history anytime

### Step 4: End Session
1. Click "End Chat" button
2. Choose to continue or end session
3. If ending, all documents are cleared
4. Chat history is saved to logs



##  Security Features

- **Session Management**: Secure Redis-based sessions
- **Input Validation**: Server-side validation for all inputs
- **CORS Protection**: Configured for specific origins
- **File Validation**: Type and size restrictions
- **SQL Injection Prevention**: Parameterized queries
- **XSS Protection**: Input sanitization

##  Logging

### Application Logs
- Location: `backend/logs/application.log`
- Rotation: Daily, 30-day retention
- Format: Timestamp, Thread, Level, Logger, Message

### User Information Logs
- Location: `backend/logs/user-info.txt`
- Contains: Name, phone, IP, location, device info

### Conversation Logs
- Location: `backend/logs/conversations.log`
- Contains: All chat messages with timestamps

##  Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## Performance Optimization

### Recommended Settings
- **Chunk Size**: 1000 tokens (balance between context and speed)
- **Top-K Retrieval**: 5 documents
- **Similarity Threshold**: 0.3
- **Temperature**: 0.1 (for factual responses)
