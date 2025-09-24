# First Word - Android News App

A minimalist, real-time news application built for Android with Firebase backend, featuring TikTok-style onboarding, community authenticity ratings, and a map-based news discovery interface.

## Overview

First Word is an MVP Android application that implements the requirements from the First Word MVP User Requirements Document. The app provides a low-friction news consumption experience with community-driven authenticity verification and location-based news discovery.

## Features

### Core Features
- **Real-time News Feed**: Chronological news wall with clear timestamps
- **Community Authenticity Ratings**: Single-vote authenticity system (True/Fake/AI)
- **Map-based News Discovery**: City-level headlines sized by engagement and authenticity
- **Social Interactions**: Like, comment, and share functionality
- **Content Creation**: Post text, images, and short videos with location tagging

### Authentication & Onboarding
- **Guest Mode**: Instant browse with limited actions, convert on intent
- **Google Sign-In**: One-tap authentication with Google SSO
- **Progressive Profile**: Handle/avatar/interests collected over time
- **Seamless Sessions**: Silent re-authentication and auto-link identities

### Technical Features
- **Firebase Integration**: Authentication, Firestore database, and Cloud Storage
- **Material Design 3**: Modern Android UI with consistent theming
- **Location Services**: Optional city-level location for map and local headlines
- **Offline Support**: Basic caching for improved performance
- **GDPR Compliance**: Privacy-focused data handling and user controls

## Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Android Views with Material Design 3
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Maps**: Google Maps SDK
- **Image Loading**: Glide
- **Architecture Pattern**: MVVM with Repository pattern
- **Build System**: Gradle with Kotlin DSL

### Project Structure
```
app/
├── src/main/java/com/firstword/app/
│   ├── ui/
│   │   ├── auth/          # Authentication screens
│   │   ├── feed/          # News feed and post interactions
│   │   ├── map/           # Map-based news discovery
│   │   ├── search/        # Search and keyword filtering
│   │   └── profile/       # User profile and settings
│   ├── models/            # Data models (User, Post, etc.)
│   ├── data/              # Repository and data access
│   └── utils/             # Utility classes and helpers
├── src/main/res/
│   ├── layout/            # XML layout files
│   ├── values/            # Strings, colors, themes
│   ├── drawable/          # Vector icons and drawables
│   └── navigation/        # Navigation graph
└── build.gradle           # App-level build configuration
```

## Firebase Configuration

### Required Firebase Services
1. **Authentication**: Google Sign-In, Anonymous Auth
2. **Firestore Database**: User profiles, posts, votes, comments
3. **Cloud Storage**: Image and video uploads
4. **Analytics**: User engagement tracking
5. **Cloud Messaging**: Push notifications (future)

### Database Schema

#### Users Collection
```kotlin
data class User(
    val id: String,
    val handle: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String,
    val authMethods: List<String>,
    val region: String,
    val interests: List<String>,
    val isGuest: Boolean,
    val privacyFlags: Map<String, Boolean>,
    val referralId: String,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int,
    val createdAt: Date?,
    val updatedAt: Date?
)
```

#### Posts Collection
```kotlin
data class Post(
    val id: String,
    val userId: String,
    val userHandle: String,
    val userDisplayName: String,
    val userAvatarUrl: String,
    val content: String,
    val imageUrls: List<String>,
    val videoUrl: String,
    val location: Location?,
    val isNews: Boolean,
    val newsSource: String,
    val newsUrl: String,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val authenticityVotes: AuthenticityVotes,
    val tags: List<String>,
    val createdAt: Date?,
    val updatedAt: Date?
)
```

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Java 17 or later
- Firebase project with required services enabled

### Installation Steps

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd FirstWordApp
   ```

2. **Firebase Setup**
   - Create a new Firebase project at https://console.firebase.google.com
   - Enable Authentication (Google Sign-In, Anonymous)
   - Create Firestore database in production mode
   - Enable Cloud Storage
   - Download `google-services.json` and place in `app/` directory

3. **Google Maps Setup**
   - Enable Google Maps SDK for Android in Google Cloud Console
   - Create API key and add to `AndroidManifest.xml`
   - Replace `YOUR_MAPS_API_KEY` with your actual API key

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   # Install on connected device or emulator
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Key Components

### MainActivity
- Navigation controller setup
- Bottom navigation management
- Authentication state checking

### FeedFragment
- News feed display with RecyclerView
- Post interaction handling (like, comment, share)
- Authenticity voting system
- Pull-to-refresh functionality

### MapFragment
- Google Maps integration
- Location-based news markers
- Engagement-based marker sizing
- User location services

### AuthActivity
- Google Sign-In implementation
- Guest mode authentication
- Progressive profile creation
- Firebase user management

### SearchFragment
- Keyword-based news search
- Trending topics display
- Search result filtering

### ProfileFragment
- User profile display
- Settings and preferences
- Sign-out functionality
- Guest user conversion prompts

## Data Models

### User Management
- Firebase Authentication integration
- Progressive profile building
- Guest user support with conversion tracking
- Privacy controls and GDPR compliance

### Content Management
- Post creation and editing
- Image/video upload to Firebase Storage
- Location tagging and privacy controls
- Content moderation hooks

### Engagement System
- Like/comment/share tracking
- Authenticity voting with fraud prevention
- Engagement scoring for map visualization
- Social graph management (followers/following)

## Security & Privacy

### Authentication Security
- Firebase Auth with secure token management
- Device-bound sessions with refresh tokens
- Anonymous authentication for guest users
- OAuth 2.0 compliance for Google Sign-In

### Data Privacy
- GDPR-compliant data handling
- User consent management
- Data export/deletion capabilities
- Minimal data collection principles
- Location data anonymization

### Content Security
- Input validation and sanitization
- Image/video content scanning hooks
- Spam and abuse detection
- Rate limiting for API calls

## Performance Optimizations

### UI Performance
- RecyclerView with ViewHolder pattern
- Image loading with Glide caching
- Lazy loading for large datasets
- Smooth animations and transitions

### Network Optimization
- Firebase offline persistence
- Efficient query pagination
- Image compression and resizing
- Background sync capabilities

### Memory Management
- Proper lifecycle management
- Image memory optimization
- Database connection pooling
- Garbage collection optimization

## Testing Strategy

### Unit Testing
- Model validation tests
- Repository layer testing
- Authentication flow testing
- Data transformation testing

### Integration Testing
- Firebase integration tests
- API endpoint testing
- Authentication flow testing
- Database operation testing

### UI Testing
- Fragment navigation testing
- User interaction testing
- Authentication flow testing
- Accessibility testing

## Deployment

### Debug Build
- Development Firebase project
- Debug signing configuration
- Logging and debugging enabled
- Test data and mock services

### Release Build
- Production Firebase project
- Release signing with keystore
- ProGuard/R8 code obfuscation
- Performance monitoring enabled

### Distribution
- Google Play Store deployment
- Firebase App Distribution for testing
- Crash reporting and analytics
- A/B testing capabilities

## Future Enhancements

### Phase 2 Features
- Facebook SSO integration
- Reputation-weighted voting system
- Sponsored posts and monetization
- Private groups and communities
- Advanced geo-personalization

### Technical Improvements
- Jetpack Compose migration
- Kotlin Multiplatform support
- Advanced caching strategies
- Real-time notifications
- Machine learning integration

## Contributing

### Development Guidelines
- Follow Android development best practices
- Use Kotlin coding conventions
- Implement proper error handling
- Write comprehensive tests
- Document public APIs

### Code Review Process
- Feature branch development
- Pull request reviews
- Automated testing requirements
- Security review for sensitive changes
- Performance impact assessment

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For technical support or questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation wiki
- Review the FAQ section

---

**First Word** - Bringing authentic, real-time news to your fingertips with community-driven verification and location-based discovery.
