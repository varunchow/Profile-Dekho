# Profile Dekho

### One place to check your coding progress.

Profile Dekho is a web application that brings coding activity from different platforms into one place.

Instead of opening multiple websites to check your progress, you can add your coding profiles and view the available statistics together in a single dashboard.

## What is Profile Dekho?

Developers often have profiles on different coding platforms, which means their progress is spread across multiple websites.

Profile Dekho was built to make this easier.

You can add your coding profile details, and the application fetches the available problem-solving information and presents it in a simple dashboard.

The idea is simple:

**Add your profiles → Fetch your stats → View your progress in one place.**

## Features

* User authentication
* Add coding platform profiles
* Fetch coding statistics
* View problems solved
* Visual representation of coding progress
* Dashboard for profile statistics
* Responsive interface
* Deployed web application

## Why I Built This

I wanted a simple way to look at my coding progress without checking every platform separately.

While building this project, I also got practical experience with authentication, fetching external data, handling API responses, connecting frontend and backend components, displaying data visually, and deploying a web application.

The project also helped me understand the small real-world problems that are usually not covered in tutorials, such as handling missing data, API failures, and different response formats.

## How It Works

```text
                User
                 |
                 v
              Login
                 |
                 v
        Add Coding Profiles
                 |
                 v
       Fetch Profile Statistics
                 |
                 v
        Process the Data
                 |
                 v
            Dashboard
                 |
                 v
       View Coding Progress
```

## Tech Stack

### Frontend

* HTML
* CSS
* JavaScript

### Backend

* Java
* Spring Boot

### Database

* Add your database here

### Deployment

* Vercel

## Project Structure

```text
Profile-Dekho/
│
├── ProfileDekho/
│   ├── frontend/
│   ├── backend/
│   └── ...
│
├── Certificates/
│
├── README.md
└── ...
```

The main application is inside the `ProfileDekho` directory.

## Getting Started

Clone the repository:

```bash
git clone https://github.com/varunchow/Profile-Dekho.git
```

Move into the project:

```bash
cd Profile-Dekho
```

Open the main project directory:

```bash
cd ProfileDekho
```

### Frontend

Install the dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

### Backend

Make sure Java and Spring Boot are installed and configured, then run the Spring Boot application.

The exact command may depend on the current backend configuration.

## Screenshots

### Login Page

```text
![Login Page](./screenshots/login.png)
```

### Dashboard

```text
![Dashboard](./screenshots/dashboard.png)
```

### Profile Statistics

```text
![Profile Statistics](./screenshots/profile-stats.png)
```

## Live Project

Live Demo:
https://profile-dekho.vercel.app/

Source Code:
https://github.com/varunchow/Profile-Dekho

## What I Learned

This project gave me practical experience with:

* Building a complete web application
* User authentication
* Working with external APIs
* Handling API responses and errors
* Connecting frontend and backend
* Designing a statistics dashboard
* Deploying an application
* Using Git and GitHub for project development

## Future Improvements

Some features I plan to work on:

* Support for more coding platforms
* Coding streak tracking
* Contest and rating history
* Historical progress charts
* Profile comparison
* Leaderboards
* Improved mobile experience
* Better error handling

## Contributing

This is mainly a personal project, but suggestions and improvements are welcome.

Feel free to open an issue or submit a pull request.

## Author

### Varun Chow

GitHub: [@varunchow](https://github.com/varunchow)

---

Built as a project to track and understand coding progress across different platforms.
