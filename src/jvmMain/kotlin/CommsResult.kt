package nl.joozd.comms

enum class CommsResult {
    OK,
    DATA_ERROR,                 // bad data received from server
    CLIENT_NOT_ALIVE,           // tried to use a client with 'alive' flag set to false
    SERVER_ERROR,               // server reported encountering an error
    CLIENT_ERROR,               // client encountered an error (eg. connection dropping halfway)
    SOCKET_IS_NULL,             // No socket created for this Client
    UNKNOWN_HOST,               // Client could not find host
    IO_ERROR,                   // Generic IO error
    CONNECTION_REFUSED,         // ConnectException occured. Typically, the connection was refused remotely (e.g., no process is listening on the remote address/port).
    SOCKET_ERROR;               // there is an error creating or accessing a Socket.
}