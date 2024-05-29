:- use_module(library(socket)).
:- use_module(library(random)).
:- use_module(library(readutil)).
:- use_module(library(apply)).

% Подключаемся к серверу по telnet и начинаем игровой цикл
start :-
    setup_call_cleanup(
        tcp_socket(Socket),
        ( tcp_connect(Socket, '127.0.0.1':3333),
          tcp_open_socket(Socket, InStream, OutStream),
          login_to_game(InStream, OutStream)
        ),
        tcp_close_socket(Socket) 
    ).

% Логинимся в игру
login_to_game(InStream, OutStream) :-
    % Читаем приветственное сообщение и введём имя пользователя
    read_line_to_string(InStream, WelcomeMessage),
    writeln(WelcomeMessage),
    
    % Отправляем имя
    write(OutStream, 'bot\n'),
    flush_output(OutStream),
    
    % Стартовый сценарий
    game_loop_body(InStream, OutStream).

% Основное тело игрового цикла
game_loop_body(InStream, OutStream) :-
    read_line_to_string(InStream, Response),
    writeln(Response),
    
    % Проверяем условия для выхода из игры
    ( substring(Response, "Game over") ->
        writeln("Game over, ending."),
        close(InStream),
        close(OutStream)
    ;   
      % Обработка других игровых сообщений и действий бота
      ( substring(Response, "You win") -> 
          writeln("You win! Ending game."),
          close(InStream),
          close(OutStream)
      ; 
      % Проверяем наличие "1." и отвечаем случайным значением от 1 до 3
      (
        ( substring(Response, "1.") ->
            send_random_number(OutStream)
        ;
        % Решаем, что делать в зависимости от ответа сервера
        ( substring(Response, "Exits:") ->
            move_to_exit(InStream, OutStream, Response)
        ;
         take_sword(OutStream),
         take_armour(OutStream),
         send_command(OutStream, "look"),
         send_command(OutStream, "inventory")
        )),
        % Рекурсивно выполняем цикл вновь
        game_loop_body(InStream, OutStream)  
      ))).

% Функция для побуждения бота двигаться к выходу
move_to_exit(_InStream, OutStream, Response) :- 
    % Может понадобится улучшить этот метод для лучших стратегий
    Substring = "north",  % Или извлекать из Response
    format(atom(Command), 'move ~w\n', [Substring]),
    write(OutStream, Command),
    flush_output(OutStream).

% Функция для взятия меча
take_sword(OutStream) :-
    send_command(OutStream, "grab sword").

% Функция для взятия брони
take_armour(OutStream) :-
    send_command(OutStream, "grab armour").

% Функция отправки команд бота на сервер игры
send_command(OutStream, Cmd) :-
    format(atom(Command), '~w\n', [Cmd]),
    write(OutStream, Command),
    flush_output(OutStream).

% Функция отправки случайного числа от 1 до 3
send_random_number(OutStream) :-
    random_between(1, 3, RandomNumber),
    format(atom(Command), '~w\n', [RandomNumber]),
    write(OutStream, Command),
    flush_output(OutStream).

:- start.
